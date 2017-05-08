package au.edu.unsw.soacourse.polling;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.jdbc.JdbcConnectionSource;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;

import au.edu.unsw.soacourse.polling.beans.Poll;
import au.edu.unsw.soacourse.polling.beans.Vote;

@Path("/polling")
public class Polling {
	private final static String DATABASE_URL = "jdbc:sqlite:polling.db";
	
	private Dao<Poll, String> pollDao;
	private Dao<Vote, String> voteDao;
	protected ConnectionSource connectionSource;
	
	@Context
	UriInfo uri;

    public Dao<Poll, String> getPollDao() {
    	try {
			connectionSource = new JdbcConnectionSource(DATABASE_URL);
		} catch (SQLException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
    	
    	if (pollDao == null) {
    		try {
				
				pollDao = DaoManager.createDao(connectionSource, Poll.class);
				if (!pollDao.isTableExists()) {
					TableUtils.createTable(connectionSource, Poll.class);
				} 
//				else {
//					TableUtils.dropTable(connectionSource, Poll.class, true);
//				}
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
    	}
		return pollDao;
	}

	public Dao<Vote, String> getVoteDao() {
    	try {
			connectionSource = new JdbcConnectionSource(DATABASE_URL);
		} catch (SQLException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
    	
    	if (voteDao == null) {
    		try {
				voteDao = DaoManager.createDao(connectionSource, Vote.class);
				if (!voteDao.isTableExists()) {
					TableUtils.createTable(connectionSource, Vote.class);
				}
//				else {
//				TableUtils.dropTable(connectionSource, Vote.class, true);
//			}
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
    	}
		return voteDao;
	}
    
//    @GET
//    @Path("/votes/")
//    @Produces("text/plain")
//    public String getVotes(@PathParam("input") String input) {
//        return input;
//    }
    
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Path("/vote")
    public Response addNewVote(
    		@FormParam("participantName") String participantName, 
    		@FormParam("chosenOption") String chosenOption,
    		@FormParam("pollId") String pollId) {
		if (participantName.isEmpty() || chosenOption.isEmpty() || pollId.isEmpty()) {
			return Response.status(Status.BAD_REQUEST).build();
		}
		
    	String voteId = UUID.randomUUID().toString();
    	try {
        	Poll poll = getPollDao().queryForId(pollId);
        	Vote outputVote = new Vote(voteId, participantName, chosenOption, poll);
			URI outputVoteUri = new URI(uri.getBaseUri() + "polling/vote/" + voteId);
			
        	if (getVoteDao().create(outputVote) > 0) 
        		connectionSource.close();
				return Response.created(outputVoteUri).build();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	
    	try {
			connectionSource.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	
    	return Response.serverError().build();
    }
    
    @GET
    @Path("/vote/{id}")
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public Response getVote(@PathParam("id") String voteId) {
        try {
        	Vote outVote = getVoteDao().queryForId(voteId);
//        	getPollDao().refresh(outVote.getPoll());

        	connectionSource.close();
        	if (outVote != null)
        		return Response.ok(outVote).build();
        	else
        		return Response.noContent().build();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        
        try {
			connectionSource.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        
        return Response.serverError().build();
    }
    
    @PUT
    @Path("/vote/{id}")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response updateVote(    		
    		@PathParam("id") String voteId,
    		@FormParam("participantName") String participantName, 
    		@FormParam("chosenOption") String chosenOption,
    		@FormParam("pollId") String pollId) {
		if (participantName.isEmpty() || chosenOption.isEmpty() || pollId.isEmpty() || voteId.isEmpty()) {
			return Response.status(Status.BAD_REQUEST).build();
		}
		
        try {	
        	Vote updatedVote = getVoteDao().queryForId(voteId);
        	
        	if (updatedVote == null) {
        		connectionSource.close();
        		return Response.noContent().build();
        	}
        	
        	Poll poll = getPollDao().queryForId(pollId);
        	
        	if (poll.getFinalChoice() != null && poll.getFinalChoice() != "") {
        		connectionSource.close();
        		return Response.status(Status.PRECONDITION_FAILED).build();
        	}
        	
        	updatedVote.setChosenOption(chosenOption);
        	updatedVote.setParticipantName(participantName);
        	updatedVote.setPoll(poll);
        	
        	getVoteDao().update(updatedVote);

        	if (getVoteDao().update(updatedVote) > 0) {
        		connectionSource.close();
        		return Response.ok().build();
        	}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        
        try {
			connectionSource.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        
        return Response.serverError().build();
    }
    
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Path("/poll")
    public Response addNewPoll(
    		@FormParam("title") String title, 
    		@FormParam("description") String description,
    		@FormParam("optionsType") String optionsType,
    		@FormParam("comments") String comments,
    		@FormParam("options") String options) {
		if (title.isEmpty() || description.isEmpty() || optionsType.isEmpty() || options.isEmpty()) {
			return Response.status(Status.BAD_REQUEST).build();
		} else if (optionsType.toUpperCase() != "GENERIC" || optionsType.toUpperCase() != "DATE") {
			return Response.status(Status.BAD_REQUEST).build();
		}
		
    	String pollId = UUID.randomUUID().toString();
    	try {
        	Poll outputPoll = new Poll(pollId, title, description, optionsType, options, comments);
			URI outputPollUri = new URI(uri.getBaseUri() + "polling/poll/" + pollId);
        	if (getPollDao().create(outputPoll) > 0) 
        		connectionSource.close();
				return Response.created(outputPollUri).build();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	
    	try {
			connectionSource.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	
    	return Response.serverError().build();
    }

    @GET
    @Path("/poll/{id}")
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public Response getPoll(@PathParam("id") String pollId) {
        try {
        	Poll outPoll = getPollDao().queryForId(pollId);
        	
        	if (outPoll != null) {
            	List<Vote> votes = getVoteDao().queryBuilder().where().eq("poll_id", outPoll).query();
            	outPoll.setVotes(votes);

            	connectionSource.close();
            	Link finaliseLink = Link.fromUri(uri.getBaseUri() + "polling/poll/finalise/" + pollId).build();
        		return Response.ok(outPoll).links(finaliseLink).build();
        	}
        	
    		connectionSource.close();
    		return Response.noContent().build();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}    	
        
        try {
			connectionSource.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	
    	return Response.serverError().build();
    }
    
    @GET
    @Path("/polls")
    @Produces("application/json")
    public Response getPolls() {
        try {
        	List<Poll> outPolls = getPollDao().queryForAll();
        	if (outPolls != null) {
        		List<Poll> retPolls = new ArrayList<Poll>();
            	for (Poll poll : outPolls) {
            		List<Vote> votes = getVoteDao().queryBuilder().where().eq("poll_id", poll.getPollId()).query();
                	poll.setVotes(votes);
                	retPolls.add(poll);
    			}
            	
            	connectionSource.close();
        		return Response.ok().entity(retPolls).build();
        	}
        	
        	connectionSource.close();
        	return Response.noContent().build();        	
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        
        try {
			connectionSource.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	
    	return Response.serverError().build();
    }
    
    @PUT
    @Path("/poll/{id}")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response updatePoll(
    		@PathParam("id") String pollId,
    		@FormParam("title") String title, 
    		@FormParam("description") String description,
    		@FormParam("optionsType") String optionsType,
    		@FormParam("comments") String comments,
    		@FormParam("options") String options,
    		@FormParam("finalChoice") String finalChoice) {
		if (title.isEmpty() || description.isEmpty() || optionsType.isEmpty() || options.isEmpty()) {
			return Response.status(Status.BAD_REQUEST).build();
		}
		
        try {
        	Poll updatedPoll = getPollDao().queryForId(pollId);
        	
        	if (updatedPoll == null) {
        		connectionSource.close();
        		return Response.noContent().build();
        	}
        	
        	List<Vote> votes = getVoteDao().queryBuilder().where().eq("poll_id", updatedPoll).query();
        	
        	if (votes.isEmpty()) {        	
	        	updatedPoll.setComments(comments);
	        	updatedPoll.setDescription(description);
	        	updatedPoll.setOptions(options);
	        	updatedPoll.setOptionType(optionsType);
	        	updatedPoll.setTitle(title);
	        	updatedPoll.setFinalChoice(finalChoice);
	        	
	        	if (getPollDao().update(updatedPoll) > 0) {
	            	connectionSource.close();
	        		return Response.ok().build();
	        	}
        	} else {
        		connectionSource.close();
        		return Response.status(Status.PRECONDITION_FAILED).build();
        	}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        
        try {
			connectionSource.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        
        return Response.serverError().build();
    }
    
    @PUT
    @Path("/poll/finalise/{id}")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response finalisePoll(
    		@PathParam("id") String pollId,
    		@FormParam("title") String title, 
    		@FormParam("description") String description,
    		@FormParam("optionsType") String optionsType,
    		@FormParam("comments") String comments,
    		@FormParam("options") String options,
    		@FormParam("finalChoice") String finalChoice) {
		if (title.isEmpty() || description.isEmpty() || optionsType.isEmpty() || options.isEmpty() || finalChoice.isEmpty()) {
			return Response.status(Status.BAD_REQUEST).build();
		}
		
        try {
        	Poll updatedPoll = getPollDao().queryForId(pollId);
        	
        	if (updatedPoll == null) {
        		connectionSource.close();
        		return Response.noContent().build();
        	}
        	
        	updatedPoll.setComments(comments);
        	updatedPoll.setDescription(description);
        	updatedPoll.setOptions(options);
        	updatedPoll.setOptionType(optionsType);
        	updatedPoll.setTitle(title);
        	updatedPoll.setFinalChoice(finalChoice);
        	
        	if (getPollDao().update(updatedPoll) > 0) {
            	connectionSource.close();
        		return Response.ok().build();
        	}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        
        try {
			connectionSource.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        
        return Response.serverError().build();
    }
    
    @DELETE
    @Path("/poll/{id}")
    public Response deletePoll(@PathParam("id") String pollId) {
        try {
        	Poll deletePoll = getPollDao().queryForId(pollId);
        	
        	if (deletePoll == null) {
        		connectionSource.close();
        		return Response.notAcceptable(null).build();
        	}
        	
        	List<Vote> votes = getVoteDao().queryBuilder().where().eq("poll_id", deletePoll).query();
        	
        	if (votes.isEmpty()) {    	
        		if (getPollDao().delete(deletePoll) > 0) {
        			connectionSource.close();
        			Link showAllPolls = Link.fromUri(uri.getBaseUri() + "polling/polls").build();
            		return Response.ok().links(showAllPolls).build();
            	}
        	} else {
        		connectionSource.close();
        		return Response.notAcceptable(null).build();
        	}
        	
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        
        try {
			connectionSource.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        
        return Response.serverError().build();
    }
    
    @GET
    @Path("/polls")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces("application/json")
    public Response searchPolls(
    		@QueryParam("title") String title,
    		@QueryParam("description") String description,
    		@QueryParam("optionType") String optionType,
    		@QueryParam("comments") String comments) {
        try {
        	if (title.isEmpty() || description.isEmpty() || optionType.isEmpty() || comments.isEmpty()) {
            	List<Poll> outPolls = getPollDao().queryForAll();
            	if (outPolls != null) {
            		List<Poll> retPolls = new ArrayList<Poll>();
                	for (Poll poll : outPolls) {
                		List<Vote> votes = getVoteDao().queryBuilder().where().eq("poll_id", poll.getPollId()).query();
                    	poll.setVotes(votes);
                    	retPolls.add(poll);
        			}
                	
                	connectionSource.close();
            		return Response.ok().entity(retPolls).build();
            	}
            	
            	connectionSource.close();
            	return Response.noContent().build();  
        	} else {
	        	Poll queryPoll = new Poll();
	        	queryPoll.setTitle(title);
	        	queryPoll.setComments(comments);
	        	queryPoll.setDescription(description);
	        	queryPoll.setOptionType(optionType);
	        	
	        	
	        	List<Poll> outPolls = getPollDao().queryForMatching(queryPoll);
	        	
	        	if (!outPolls.isEmpty()) {
	        		List<Poll> retPolls = new ArrayList<Poll>();
	            	for (Poll poll : outPolls) {
	            		List<Vote> votes = getVoteDao().queryBuilder().where().eq("poll_id", poll.getPollId()).query();
	                	poll.setVotes(votes);
	                	retPolls.add(poll);
	    			}
	            	
	            	connectionSource.close();
	        		return Response.ok().entity(retPolls).build();
	        	}
	        	
	        	connectionSource.close();
	        	return Response.noContent().build();  
        	}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

        try {
			connectionSource.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        
        return Response.serverError().build();
    }
}
