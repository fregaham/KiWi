/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright (c) 2008-2009, The KiWi Project (http://www.kiwi-project.eu)
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without 
 * modification, are permitted provided that the following conditions are met:
 * - Redistributions of source code must retain the above copyright notice, 
 *   this list of conditions and the following disclaimer.
 * - Redistributions in binary form must reproduce the above copyright notice, 
 *   this list of conditions and the following disclaimer in the documentation 
 *   and/or other materials provided with the distribution.
 * - Neither the name of the KiWi Project nor the names of its contributors 
 *   may be used to endorse or promote products derived from this software 
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" 
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE 
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE 
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE 
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR 
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF 
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS 
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN 
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) 
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE 
 * POSSIBILITY OF SUCH DAMAGE.
 * 
 * Contributor(s):
 * 
 * 
 */
package kiwi.service.reasoning;

import java.util.HashSet;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;

import kiwi.api.event.KiWiEvents;
import kiwi.api.reasoning.ProgramProcessor;
import kiwi.api.reasoning.ReasoningServiceLocal;
import kiwi.api.reasoning.ReasoningServiceRemote;
import kiwi.api.system.StatusService;
import kiwi.api.transaction.TransactionService;
import kiwi.api.transaction.UpdateTransactionBean;
import kiwi.context.CurrentUserFactory;
import kiwi.model.content.ContentItem;
import kiwi.model.kbase.KiWiTriple;
import kiwi.model.status.SystemStatus;
import kiwi.service.reasoning.ReasoningConfiguration.ReasoningFeature;
import kiwi.service.reasoning.ReasoningTask.ReasoningTaskType;
import kiwi.service.reasoning.ast.Program;
import kiwi.service.reasoning.util.ReasoningUtils;
import kiwi.service.transaction.KiWiSynchronizationImpl;

import org.jboss.seam.Component;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.AutoCreate;
import org.jboss.seam.annotations.Create;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Observer;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.log.Log;
import org.jboss.seam.transaction.Transaction;

/** ReasoningImpl implements the user facing reasoning service.
 * 
 * It takes care of processing triple store update events, it turns them into ReasoningTasks and enqueues them.
 * Also provides some basic information about inferred facts.
 * 
 * @author Jakub Kotowski
 *
 */
@Name("kiwi.core.reasoningService")
@Scope(ScopeType.APPLICATION)
@AutoCreate
public class ReasoningImpl implements ReasoningServiceLocal, ReasoningServiceRemote {	
	@Logger
	private Log log;
	
	@In
	private ReasoningConfiguration reasoningConfiguration;
	
	@In("kiwi.core.statusService")
	private StatusService statusService;

	@In 
	private ProgramProcessor programProcessor;
	
	@In
	private TriplestoreProgramPersister programPersister;
	@In
	private TriplestoreProgramLoader triplestoreProgramLoader;
	@In
	private ClasspathProgramLoader classpathProgramLoader;

	@In
	private TriplestoreTBoxLoader triplestoreTBoxLoader;
	
//	@In
//	private TriplestoreNewTagListener triplestoreNewTagListener;

	@In("TBox")
	private TBox tbox;

	private int taskCounter = 0;
	private boolean running;
	private Queue<ReasoningTask> taskQueue;
	private Lock reasonerLock;
	private ReasoningStatistics stats;
	private ReasoningTask currentTask;	
	
	@In
	private TransactionService transactionService;

	boolean initialized = false;

	private void loadPrograms() {
		
		for (String program : ReasoningConstants.PREDEFINED_PROGRAMS) {		
			Program defaultProgram = classpathProgramLoader.loadProgram(program);
			Program storedProgram = triplestoreProgramLoader.loadProgram(program);
			
			if (storedProgram == null) {
				programPersister.persist(defaultProgram);
				continue;
			}
			
			ProgramUpdateTask update = ProgramComparer.compare(defaultProgram, storedProgram);
			
			if (!update.isEmpty()) {
				log.info("Program #0 has changed: #1", storedProgram.getName(), update.toString());
				enqueueTask(update);
				log.info("Maybe it was the defaultProgram that changed, persisting it again.");
				programPersister.persist(defaultProgram);				
			}
		}
					
	}
	
	@Create
	public void create() {
		taskQueue = new ConcurrentLinkedQueue<ReasoningTask>();
		reasonerLock = new ReentrantLock();
		stats = new ReasoningStatistics();		
	}
	
	/**
	 * 
	 * We are creating two connections (not only two sessions) for the ReasoningImpl <-> Reasoner communication because it is convenient.
	 * It seems (documentation of Connection?) that a more standard solution would be to use just one Connection to create two Sessions.
	 */
	public void init() {
		
		synchronized(this) {
			if(!initialized) {
				CurrentUserFactory currentUserFactory = 
					(CurrentUserFactory) Component.getInstance("currentUserFactory");
				currentUserFactory.forceRefresh();

				log.info("Initializing reasoning service.");
				SystemStatus status = new SystemStatus("Initializing reasoning.");
				status.setId("reasonerStatus");
				statusService.addSystemStatus(status);

				boolean enabled = isReasoningEnabled();
				disableReasoning(); //we don't want any reasoning to happen until we load some program

				status.setProgress(0);
				status.setMessage("Loading programs.");
				loadPrograms();

				if (reasoningConfiguration.isEnabled(ReasoningFeature.HYBRID_REASONING)) {
					status.setProgress(50);
					status.setMessage("Loading TBox.");
					triplestoreTBoxLoader.load();
				}

				status.setMessage("Reasoning initialized.");
				status.setProgress(100);

				setReasoningEnabled(enabled);

				statusService.removeSystemStatus(status);
				
				initialized = true;
			}
		}
		//runEnqueuedTasks();
	} 

	
	
	
	/**
	 * Enqueue a reasoning task; if the reasoner is not already running, start it and let it run
	 * while the queue is not empty, so that new tasks can be added in the queue and proceed without
	 * blocking.
	 * 
	 * Perhaps this is not necessary, because we are anyways working with asynchronous events...
	 * @param t
	 */
	public void enqueueTask(ReasoningTask t) {
		taskCounter++;
		t.setNumber(taskCounter);
		taskQueue.add(t);
		
		runEnqueuedTasks();
	}
	
	public void runEnqueuedTasks() {
		try {
			Transaction.instance().begin();
			transactionService.registerSynchronization(
					KiWiSynchronizationImpl.getInstance(), 
					transactionService.getUserTransaction());
			init();
			Transaction.instance().commit();
		} catch (SecurityException e1) {
			e1.printStackTrace();
		} catch (IllegalStateException e1) {
			e1.printStackTrace();
		} catch (NotSupportedException e1) {
			e1.printStackTrace();
		} catch (SystemException e1) {
			e1.printStackTrace();
		} catch (RollbackException e1) {
			e1.printStackTrace();
		} catch (HeuristicMixedException e1) {
			e1.printStackTrace();
		} catch (HeuristicRollbackException e1) {
			e1.printStackTrace();
		}
		
		// run reasoner if it is not yet running
		if(isReasoningEnabled() && !running) {
			
			// ensure that only one reasoner is started at the same time
			reasonerLock.lock();
			
			try {
				running = true;
				
				
				// run reasoner as long as there are tasks in the queue; if new tasks are added 
				// while the reasoner is still running, they are executed in subsequent iterations
				// of the loop
				while(!taskQueue.isEmpty() && isReasoningEnabled()) {
					SystemStatus status = new SystemStatus("Reasoner ("+taskQueue.size()+ (taskQueue.size() > 1 ? " tasks remaining)": " task remaining)"));
					status.setId("reasonerStatus");
					statusService.addSystemStatus(status);
					
					currentTask = taskQueue.poll();					
					
					if(currentTask == null) {
						log.error("task is null!");
					}
					
					try {
						processTask(currentTask);
					} catch (SecurityException e) {
						log.error("error while committing transaction (security exception)",e);
					} catch (IllegalStateException e) {
						log.error("error while committing transaction (illegal state)",e);
					} finally {
						statusService.removeSystemStatus(status);						
					}
											
				}
			} finally {
				
				// release lock
				running = false;
				reasonerLock.unlock();
			}
		} else if(!isReasoningEnabled()) {
			log.info("online reasoning disabled; just enqueuing reasoning task without starting reasoner");
		} else if(running) {
			log.info("reasoner already running; just enqueuing reasoning task");
		}
		
	}
	
	/** Processes the queued triples.
	 * 
	 * 
	 */
	@Observer(value=KiWiEvents.TRANSACTION_SUCCESS+"Async",create=true)
	public void scheduleTransaction(UpdateTransactionBean utb) { 
		Set<KiWiTriple> addedTriples = new HashSet<KiWiTriple>(), removedTriples = new HashSet<KiWiTriple>();
		synchronized(utb) {
			for(ContentItem ci : utb.getContentItemVersionMap().keySet()) {
				addedTriples.addAll(ReasoningUtils.getNewTriples(
						new HashSet<KiWiTriple>(
								utb.getContentItemVersionMap().get(ci).getTransactionAddedBaseTriples())) );
				removedTriples.addAll(ReasoningUtils.getNewTriples(
						new HashSet<KiWiTriple>(
								utb.getContentItemVersionMap().get(ci).getTransactionRemovedBaseTriples())) );				
			}
			
			if (addedTriples.isEmpty() && removedTriples.isEmpty()) {
				log.debug("Skipping reasoning echoes ...");
				return;
			}
			
			if (!addedTriples.isEmpty())
				enqueueTask(new AddTriplesTask(addedTriples));
			
			if (!removedTriples.isEmpty())
				enqueueTask(new RemoveTriplesTask(removedTriples));			
		}
	}
		
	/** Runs reasoning on the whole triplestore.
	 * 
	 * Temporarily enables online reasoning if it was disabled. First processes all tasks remaining in the queue and then
	 * starts the whole triplestore reasoning.
	 * 
	 */
	public void runReasoner() {
		ReasoningTask task = new RunReasoningTask();
		enqueueTask(task);
		
		if (!isReasoningEnabled()) {
			enableReasoning();
			
			runEnqueuedTasks();
			
			disableReasoning();
		}
	}
	
	public void processTask(ReasoningTask t) { 
		log.info("Received task #0.", t.toString());
		
		SystemStatus status = statusService.getSystemStatus("reasonerStatus");

		//TODO added and removed have to be processed at once by one ruleProcessor method for the ruleProcessor.isWorking() really work in processScheduledTasks()
		ReasoningTaskStatistics taskStats = null;
		ConfigurationChangeTask task;
		
		switch (t.getReasoningTaskType()) {
		case ADD_TRIPLES:
			Set<KiWiTriple> addedTriples = ((AddTriplesTask)t).getAddedTriples();
			
			log.info("Processing #0 new triples.",addedTriples.size());
			//ReasoningUtils.printSetToLog(addedTriples, log);
			
			//we have to pass on a copy of the queue... but is a shallow copy sufficient?!
			
			taskStats = programProcessor.processAddedTriples(addedTriples); 
			
			//triplestoreNewTagListener.processAdded(addedTriples);
			
			log.info("#0 new explicit triples resulted in #1 new inferred triples. The overall processing took #2 ms. Details follow.", addedTriples.size(), stats.getNewTriplesCount(), stats.getTime());
			if(taskStats != null) {
				log.info("#0", taskStats.toString());
			} else {
				log.error("taskStats are null");
			}
			break;
		case REMOVE_TRIPLES:
			Set<KiWiTriple> removedTriples = ((RemoveTriplesTask)t).getRemovedTriples();
			log.info("Processing #0 removed triples.",removedTriples.size());

			taskStats = programProcessor.processRemovedTriples(removedTriples);
			
			//triplestoreNewTagListener.processRemoved(removedTriples);
			
			log.info("Removing triples took #0 ms.", stats.getTime());
			break;
		case RUN_REASONING:
			taskStats = programProcessor.processAddedTriples(null); 
			log.info("Reasoning took #0 ms. Details follow.", stats.getTime());
			log.info("#0", taskStats.toString());
			break;
		case PROGRAM_UPDATE:
			log.warn("!!! Program updates are not implemented yet. The task was: "+t.toString());
			break;
		case ENABLE_FEATURE:
			task = (ConfigurationChangeTask) t;
			taskStats = new ReasoningTaskStatistics();
			taskStats.setReasoningTaskDescription(task.toString());

			switch (task.getFeature()) {
			case HYBRID_REASONING:
				taskStats.start();
				status.setMessage("Loading TBox.");
				triplestoreTBoxLoader.load();

				status.setMessage("Enabling hybrid reasoning.");
				
				reasoningConfiguration.setEnabled(ReasoningFeature.HYBRID_REASONING, true);
				break;
			case ONLINE_REASONING:
				//this can be processed only if online reasoning is enabled... but what the heck..
				reasoningConfiguration.setEnabled(ReasoningFeature.ONLINE_REASONING, true);
				break;
				default:
					throw new ReasoningException("Enabling feature "+task.getFeature()+" via tasks is not implemented.");
			}
			taskStats.stop();
			break;
		case DISABLE_FEATURE:
			task = (ConfigurationChangeTask) t;
			taskStats = new ReasoningTaskStatistics();
			taskStats.setReasoningTaskDescription(task.toString());

			switch (task.getFeature()) {
			case HYBRID_REASONING:
				reasoningConfiguration.setEnabled(ReasoningFeature.HYBRID_REASONING, false);
				tbox.clear();
				break;
			case ONLINE_REASONING:
				reasoningConfiguration.setEnabled(ReasoningFeature.ONLINE_REASONING, false);
				break;
				default:
					throw new ReasoningException("Disabling feature "+task.getFeature()+" via tasks is not implemented.");
			}
			
			taskStats.stop();
			break;
		default:
				throw new ReasoningException("Reasoning task type "+t.getReasoningTaskType()+" is not supported.");
		}
		
		if (taskStats != null) {
			taskStats.setReasoningTaskNumber(t.getNumber());
			taskStats.setReasoningTaskDescription(t.toString());
			stats.addTaskStatistics(taskStats);
		}
	}	
	
	public boolean isReasonerRunning() {
		return running;
	}

	public Queue<ReasoningTask> getTaskQueue() {
		return taskQueue;
	}

	public void setTaskQueue(Queue<ReasoningTask> taskQueue) {
		this.taskQueue = taskQueue;
	}
	
	public ReasoningStatistics getReasoningStatistics() {
		return stats;
	}
	
	public ReasoningTask getCurrentTask() {
		return currentTask.clone();
	}

	public void disableReasoning() {
		setReasoningEnabled(false);
	}

	public void enableReasoning() {
		setReasoningEnabled(true);
	}

	public boolean isReasoningEnabled() {
		return reasoningConfiguration.isEnabled(ReasoningFeature.ONLINE_REASONING);
	}

	public void setReasoningEnabled(boolean enabled) {
		reasoningConfiguration.setEnabled(ReasoningFeature.ONLINE_REASONING, enabled);
	}
		
	public void setHybridReasoningEnabled(boolean enabled) {
		ReasoningTaskType task = enabled ? ReasoningTaskType.ENABLE_FEATURE : ReasoningTaskType.DISABLE_FEATURE;
		
		enqueueTask(new ConfigurationChangeTask(task, ReasoningFeature.HYBRID_REASONING));
	}
	
	public boolean isHybridReasoningEnabled() {
		return reasoningConfiguration.isEnabled(ReasoningFeature.HYBRID_REASONING);
	}
}




