CREATE OR REPLACE FUNCTION calculate_equity(create_time timestamp, action_time timestamp, unit_value double precision, action_type character varying) RETURNS double precision AS $$

DECLARE
    result double precision;
BEGIN
    IF action_type = 'ACCESS_DENIED' THEN
        result = accessDeniedAging(create_time, action_time, unit_value);
    end IF;

    IF action_type = 'ADD_FRIEND' THEN
        result = addFriendAging(create_time, action_time, unit_value);
    end IF;

    IF action_type = 'ADDTYPE' THEN
        result = addtypeAging(create_time, action_time, unit_value);
    end IF;

    IF action_type = 'ANNOTATE' THEN
        result = annotateAging(create_time, action_time, unit_value);
    end IF;

    IF action_type = 'CHANGE_AUTHOR' THEN
        result = changeAuthorAging(create_time, action_time, unit_value);
    end IF;

    IF action_type = 'COMMENT' THEN
        result = commentAging(create_time, action_time, unit_value);
    end IF;
    
    IF action_type = 'CREATE' THEN
        result = createAging(create_time, action_time, unit_value);
    end IF;

    IF action_type = 'DELETE' THEN
        result = deleteAging(create_time, action_time, unit_value);
    end IF;

    IF action_type = 'EDIT' THEN
        result = editAging(create_time, action_time, unit_value);
    end IF;

    IF action_type = 'GROUP_CREATED' THEN
        result = groupCreatedAging(create_time, action_time, unit_value);
    end IF;

    IF action_type = 'LOGIN' THEN
        result = loginAging(create_time, action_time, unit_value);
    end IF;

    IF action_type = 'LOGOUT' THEN
        result = logoutAging(create_time, action_time, unit_value);
    end IF;

    IF action_type = 'RATE_CONTENTITEM' THEN
        result = rateAging(create_time, action_time, unit_value);
    end IF;

    IF action_type = 'REGISTER' THEN
        result = registerAging(create_time, action_time, unit_value);
    end IF;

    IF action_type = 'REMOVE_FRIEND' THEN
        result = removeFriendAging(create_time, action_time, unit_value);
    end IF;

    IF action_type = 'SEARCH' THEN
        result = searchAging(create_time, action_time, unit_value);
    end IF;

    IF action_type = 'SHARE' THEN
        result = shareAging(create_time, action_time, unit_value);
    end IF;

    IF action_type = 'TWEET' THEN
        result = tweetAging(create_time, action_time, unit_value);
    end IF;

    IF action_type = 'USER_CREATED' THEN
        result = userCreatedAging(create_time, action_time, unit_value);
    end IF;

    IF action_type = 'USER_REMOVED' THEN
        result = userRemovedAging(create_time, action_time, unit_value);
    end IF;

    IF action_type = 'VISIT' THEN
        result = visitAging(create_time, action_time, unit_value);
     ELSE
        result = generalAging(create_time, action_time, unit_value);
    end IF;

    RETURN result;
END;

$$ LANGUAGE plpgsql;