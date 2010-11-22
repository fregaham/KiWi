package kiwi.action.rating;

import kiwi.api.rating.RatingService;
import kiwi.model.content.ContentItem;
import kiwi.model.user.User;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.log.Log;
import org.primefaces.event.RateEvent;

@Name("ratingAction")
@Scope(ScopeType.PAGE)
public class RatingAction {
	
	@Logger
	private Log log;
	
	@In(create=true)
	ContentItem currentContentItem;
	
	@In
	User currentUser;
	
	@In
	RatingService ratingService;
	
	private Double rate;
	
	public double getPrimeRating() {
		if(rate == null) {
			rate = ratingService.getRatingAverage(currentContentItem);
		}
		return rate;
	}

	public void handlePrimeRating(RateEvent rateEvent) {
		Double rating = rateEvent.getRating();
		if( rating != null ) {
			ratingService.setRating(currentContentItem, currentUser, rating.intValue());
			log.info("set rating to #0", rating);
		} else {
			ratingService.cancelRating(currentContentItem, currentUser);
			log.info("clear rating");
		}
		rate = ratingService.getRatingAverage(currentContentItem);
	}
	
}
