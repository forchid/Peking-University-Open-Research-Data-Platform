package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetFieldType;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.DataverseContact;
import edu.harvard.iq.dataverse.authorization.DataverseRole;
import edu.harvard.iq.dataverse.DvObject;
import edu.harvard.iq.dataverse.MetadataBlock;
import edu.harvard.iq.dataverse.RoleAssignment;
import edu.harvard.iq.dataverse.api.dto.ExplicitGroupDTO;
import edu.harvard.iq.dataverse.api.dto.RoleAssignmentDTO;
import edu.harvard.iq.dataverse.api.dto.RoleDTO;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.authorization.RoleAssignee;
import edu.harvard.iq.dataverse.authorization.groups.impl.explicit.ExplicitGroup;
import edu.harvard.iq.dataverse.authorization.groups.impl.explicit.ExplicitGroupProvider;
import edu.harvard.iq.dataverse.authorization.groups.impl.explicit.ExplicitGroupServiceBean;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.authorization.users.User;
import edu.harvard.iq.dataverse.engine.command.impl.AddRoleAssigneesToExplicitGroupCommand;
import edu.harvard.iq.dataverse.engine.command.impl.AssignRoleCommand;
import edu.harvard.iq.dataverse.engine.command.impl.CreateDatasetCommand;
import edu.harvard.iq.dataverse.engine.command.impl.CreateDataverseCommand;
import edu.harvard.iq.dataverse.engine.command.impl.CreateExplicitGroupCommand;
import edu.harvard.iq.dataverse.engine.command.impl.CreateRoleCommand;
import edu.harvard.iq.dataverse.engine.command.impl.DeleteDataverseCommand;
import edu.harvard.iq.dataverse.engine.command.impl.DeleteExplicitGroupCommand;
import edu.harvard.iq.dataverse.engine.command.impl.GetDataverseCommand;
import edu.harvard.iq.dataverse.engine.command.impl.GetExplicitGroupCommand;
import edu.harvard.iq.dataverse.engine.command.impl.ListDataverseContentCommand;
import edu.harvard.iq.dataverse.engine.command.impl.ListExplicitGroupsCommand;
import edu.harvard.iq.dataverse.engine.command.impl.ListMetadataBlocksCommand;
import edu.harvard.iq.dataverse.engine.command.impl.ListRoleAssignments;
import edu.harvard.iq.dataverse.engine.command.impl.ListRolesCommand;
import edu.harvard.iq.dataverse.engine.command.impl.PublishDataverseCommand;
import edu.harvard.iq.dataverse.engine.command.impl.RemoveRoleAssigneesFromExplicitGroupCommand;
import edu.harvard.iq.dataverse.engine.command.impl.RevokeRoleCommand;
import edu.harvard.iq.dataverse.engine.command.impl.UpdateDataverseCommand;
import edu.harvard.iq.dataverse.engine.command.impl.UpdateDataverseMetadataBlocksCommand;
import edu.harvard.iq.dataverse.engine.command.impl.UpdateExplicitGroupCommand;
import edu.harvard.iq.dataverse.util.json.JsonParseException;
import static edu.harvard.iq.dataverse.util.json.JsonPrinter.brief;
import static edu.harvard.iq.dataverse.util.json.JsonPrinter.json;
import java.io.StringReader;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.ejb.Stateless;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonString;
import javax.json.JsonValue;
import javax.json.JsonValue.ValueType;
import javax.json.stream.JsonParsingException;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

/**
 * A REST API for dataverses.
 * @author michael
 */
@Stateless
@Path("dataverses")
public class Dataverses extends AbstractApiBean {
       
	private static final Logger logger = Logger.getLogger(Dataverses.class.getName());

	@POST
	public Response addRoot( String body, @QueryParam("key") String apiKey ) {
        logger.info("Creating root dataverse");
		return addDataverse( body, "", apiKey);
	}
	
	@POST
	@Path("{identifier}")
	public Response addDataverse( String body, @PathParam("identifier") String parentIdtf, @QueryParam("key") String apiKey) {
		
        Dataverse d;
        JsonObject dvJson;
        try ( StringReader rdr = new StringReader(body) ) {
            dvJson = Json.createReader(rdr).readObject();
            d = jsonParser().parseDataverse(dvJson);
        } catch ( JsonParsingException jpe ) {
            logger.log(Level.SEVERE, "Json: {0}", body);
            return errorResponse( Status.BAD_REQUEST, "Error parsing Json: " + jpe.getMessage() );
        } catch (JsonParseException ex) {
            Logger.getLogger(Dataverses.class.getName()).log(Level.SEVERE, "Error parsing dataverse from json: " + ex.getMessage(), ex);
            return errorResponse( Response.Status.BAD_REQUEST,
                    "Error parsing the POSTed json into a dataverse: " + ex.getMessage() );
        }
        
		try {
            if ( ! parentIdtf.isEmpty() ) {
                Dataverse owner = findDataverseOrDie( parentIdtf );
                d.setOwner(owner);
            }

            // set the dataverse - contact relationship in the contacts
            for (DataverseContact dc : d.getDataverseContacts()) {
                dc.setDataverse(d);
            }

            AuthenticatedUser u = findUserOrDie( apiKey );
            d = execCommand( new CreateDataverseCommand(d, u, null, null), "Creating Dataverse" );
			return createdResponse( "/dataverses/"+d.getAlias(), json(d) );
        } catch ( WrappedResponse ww ) {
            return ww.getResponse();
            
        } catch (EJBException ex) {
            Throwable cause = ex;
            StringBuilder sb = new StringBuilder();
            sb.append("Error creating dataverse.");
            while (cause.getCause() != null) {
                cause = cause.getCause();
                if (cause instanceof ConstraintViolationException) {
                    ConstraintViolationException constraintViolationException = (ConstraintViolationException) cause;
                    for (ConstraintViolation<?> violation : constraintViolationException.getConstraintViolations()) {
                        sb.append(" Invalid value: <<<").append(violation.getInvalidValue()).append(">>> for ")
                                .append(violation.getPropertyPath()).append(" at ")
                                .append(violation.getLeafBean()).append(" - ")
                                .append(violation.getMessage());
                    }
                }
            }
            logger.log(Level.SEVERE, sb.toString());
            return errorResponse( Response.Status.INTERNAL_SERVER_ERROR, "Error creating dataverse: " + sb.toString() );
        } catch ( Exception ex ) {
			logger.log(Level.SEVERE, "Error creating dataverse", ex);
			return errorResponse( Response.Status.INTERNAL_SERVER_ERROR, "Error creating dataverse: " + ex.getMessage() );
            
        }
	}
    
    @POST
    @Path("{identifier}/datasets")
    public Response createDataset( String jsonBody, @PathParam("identifier") String parentIdtf, @QueryParam("key") String apiKey ) {
        try {
            User u = findUserOrDie(apiKey);
            Dataverse owner = findDataverseOrDie(parentIdtf);
            
            JsonObject json;
            try ( StringReader rdr = new StringReader(jsonBody) ) {
                json = Json.createReader(rdr).readObject();
            } catch ( JsonParsingException jpe ) {
                logger.log(Level.SEVERE, "Json: {0}", jsonBody);
                return errorResponse( Status.BAD_REQUEST, "Error parsing Json: " + jpe.getMessage() );
            }
            
            Dataset ds = new Dataset();
            ds.setOwner(owner);
          
         
            JsonObject jsonVersion = json.getJsonObject("datasetVersion");
            if ( jsonVersion == null) {
                return errorResponse(Status.BAD_REQUEST, "Json POST data are missing datasetVersion object.");
            }
            try {
                try {
                    DatasetVersion version = jsonParser().parseDatasetVersion(jsonVersion);
                    
                    // force "initial version" properties
                    version.setMinorVersionNumber(null);
                    version.setVersionNumber(null);
                    version.setVersionState(DatasetVersion.VersionState.DRAFT);
                    LinkedList<DatasetVersion> versions = new LinkedList<>();
                    versions.add(version);
                    version.setDataset(ds);
                    
                    ds.setVersions( versions );
                } catch ( javax.ejb.TransactionRolledbackLocalException rbe ) {
                    throw rbe.getCausedByException();
                }
            } catch (JsonParseException ex) {
                logger.log( Level.INFO, "Error parsing dataset version from Json", ex);
                return errorResponse(Status.BAD_REQUEST, "Error parsing datasetVersion: " + ex.getMessage() );
            } catch ( Exception e ) {
                logger.log( Level.WARNING, "Error parsing dataset version from Json", e);
                return errorResponse(Status.INTERNAL_SERVER_ERROR, "Error parsing datasetVersion: " + e.getMessage() );
            }
            
            Dataset managedDs = execCommand(new CreateDatasetCommand(ds, u), "Creating Dataset");
            return createdResponse( "/datasets/" + managedDs.getId(),
                    Json.createObjectBuilder().add("id", managedDs.getId()) );
                
        } catch ( WrappedResponse ex ) {
            return ex.getResponse();
        }
    }
	
	@GET
	@Path("{identifier}")
	public Response viewDataverse( @PathParam("identifier") String idtf, @QueryParam("key") String apiKey ) {
        try {
            Dataverse d = findDataverseOrDie(idtf);
            User u = findUserOrDie(apiKey);
			Dataverse retrieved = execCommand( new GetDataverseCommand(u, d), "Get Dataverse" );
			return okResponse( json(retrieved) );
		} catch ( WrappedResponse ex ) {
			return ex.getResponse();
		}
	}
	
	@DELETE
	@Path("{identifier}")
	public Response deleteDataverse( @PathParam("identifier") String idtf, @QueryParam("key") String apiKey ) {
		try {
            Dataverse d = findDataverseOrDie(idtf);
            User u = findUserOrDie(apiKey);
			execCommand( new DeleteDataverseCommand(u, d), "Delete Dataverse" );
			return okResponse( "Dataverse " + idtf  +" deleted");
		} catch ( WrappedResponse ex ) {
			return ex.getResponse();
		}
	}
	
	@GET
	@Path("{identifier}/metadatablocks")
	public Response listMetadataBlocks( @PathParam("identifier") String dvIdtf, @QueryParam("key") String apiKey ) {
        try {
            User u = findUserOrDie(apiKey);
            Dataverse dataverse = findDataverseOrDie(dvIdtf);
            
            JsonArrayBuilder jab = Json.createArrayBuilder();
            for ( MetadataBlock blk : execCommand( new ListMetadataBlocksCommand(u, dataverse), 
                                                    "Listing Metadata blocks for dataverse " + dvIdtf)){
                jab.add( brief.json(blk) );
            }
            
            return okResponse(jab);
            
        } catch (WrappedResponse ex) {
            return ex.getResponse();
        }
	}
	
    @POST
    @Path("{identifier}/metadatablocks")
    @Produces(MediaType.APPLICATION_JSON)
    public Response setMetadataBlocks( @PathParam("identifier")String dvIdtf, @QueryParam("key") String apiKey, String blockIds ) {
        
        List<MetadataBlock> blocks = new LinkedList<>();
        try {
            for ( JsonValue blockId : Util.asJsonArray(blockIds).getValuesAs(JsonValue.class) ) {
                MetadataBlock blk = (blockId.getValueType()==ValueType.NUMBER)
                                        ? findMetadataBlock( ((JsonNumber)blockId).longValue() )
                                        : findMetadataBlock( ((JsonString)blockId).getString() );
                if ( blk == null ) {
                    return errorResponse(Response.Status.BAD_REQUEST, "Can't find metadata block '"+ blockId + "'");
                }
                blocks.add( blk );
            }
        } catch( Exception e ) {
            return errorResponse(Response.Status.BAD_REQUEST, e.getMessage());
        }
        
        try {
            User u = findUserOrDie(apiKey);
            Dataverse dataverse = findDataverseOrDie(dvIdtf);
            execCommand( new UpdateDataverseMetadataBlocksCommand.SetBlocks(u, dataverse, blocks),
                    "updating metadata blocks for dataverse " + dvIdtf );
            return okResponse("Metadata blocks of dataverse " + dvIdtf + " updated.");
            
        } catch (WrappedResponse ex) {
            return ex.getResponse();
        }
    }
 
    @GET
    @Path("{identifier}/metadatablocks/:isRoot")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getMetadataRoot( @PathParam("identifier")String dvIdtf, @QueryParam("key") String apiKey  ) {
        
        try {
            User u = findUserOrDie(apiKey);
            Dataverse dataverse = findDataverseOrDie(dvIdtf);
            if ( permissionSvc.on(dataverse).user(u).has(Permission.EditDataverse) ) {
                return okResponseWithValue( dataverse.isMetadataBlockRoot() );
            } else {
                return errorResponse( Status.FORBIDDEN, "Not authorized" );
            }
        } catch (WrappedResponse wr) {
            return wr.getResponse();
        }
    }
    
    @POST
    @Path("{identifier}/metadatablocks/:isRoot")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.WILDCARD)
    public Response setMetadataRoot( @PathParam("identifier")String dvIdtf, @QueryParam("key") String apiKey, String body  ) {
        
        if ( ! Util.isBoolean(body) ) {
            return errorResponse(Response.Status.BAD_REQUEST, "Illegal value '" + body + "'. Try 'true' or 'false'");
        }
        boolean root = Util.isTrue(body);
        
        try {
            User u = findUserOrDie(apiKey);
    		Dataverse dataverse = findDataverseOrDie(dvIdtf);
            execute(new UpdateDataverseMetadataBlocksCommand.SetRoot(u, dataverse, root));
            return okResponseWithValue("Dataverse " + dataverse.getName() + " is now a metadata root");
        } catch (WrappedResponse wr) {
            return wr.getResponse();
        }

    }
    
    @POST
    @Path("{identifier}/facets")
    @Produces(MediaType.APPLICATION_JSON)
    public Response setFacets( @PathParam("identifier")String dvIdtf, @QueryParam("key") String apiKey, String facetIds ) {
        
        List<DatasetFieldType> facets = new LinkedList<>();
        for ( JsonString facetId : Util.asJsonArray(facetIds).getValuesAs(JsonString.class) ) {
            DatasetFieldType dsfType = findDatasetFieldType(facetId.getString());
            if ( dsfType == null ) {
                return errorResponse(Response.Status.BAD_REQUEST, "Can't find dataset field type '"+ facetId + "'");
            } else if (!dsfType.isFacetable()) {
                return errorResponse(Response.Status.BAD_REQUEST, "Dataset field type '"+ facetId + "' is not facetable");              
            }
            facets.add( dsfType );
        }
        
        try {
            User u = findUserOrDie(apiKey);
            Dataverse dataverse = findDataverseOrDie(dvIdtf);
            // by passing null for Featured Dataverses and DataverseFieldTypeInputLevel, those are not changed
            execCommand( new UpdateDataverseCommand(dataverse, facets, null, u, null),
                    "updating facets for dataverse " + dvIdtf );
            return okResponse("Facets of dataverse " + dvIdtf + " updated.");
            
        } catch (WrappedResponse ex) {
            return ex.getResponse();
        }
    }    
    
	@GET
	@Path("{identifier}/contents")
	public Response listContent( @PathParam("identifier") String dvIdtf, @QueryParam("key") String apiKey ) {

        final JsonArrayBuilder jab = Json.createArrayBuilder();
		DvObject.Visitor<Void> ser = new DvObject.Visitor<Void>() {

			@Override
			public Void visit(Dataverse dv) {
				jab.add( Json.createObjectBuilder().add("type", "dataverse")
						.add("id", dv.getId())
						.add("title",dv.getName() ));
				return null;
			}

			@Override
			public Void visit(Dataset ds) {
                // TODO: check for permission to view drafts
				jab.add( json(ds).add("type", "dataset") );
				return null;
			}

			@Override
			public Void visit(DataFile df) { throw new UnsupportedOperationException("Files don't live directly in Dataverses"); }
		};
        
		try {
            Dataverse dataverse = findDataverseOrDie(dvIdtf);
            User u = findUserOrDie(apiKey);

			for ( DvObject o : execCommand(new ListDataverseContentCommand(u, dataverse), "List Dataverse") ) {
				o.accept(ser);
			}
            return okResponse(jab);
		} catch (WrappedResponse ex) {
			return ex.getResponse();
		}
	}
	
    @GET
	@Path("{identifier}/roles")
	public Response listRoles( @PathParam("identifier") String dvIdtf, @QueryParam("key") String apiKey ) {

        try {
            Dataverse d = findDataverseOrDie(dvIdtf);
            User u = findUserOrDie(apiKey);
            JsonArrayBuilder jab = Json.createArrayBuilder();
            for ( DataverseRole r : execCommand( new ListRolesCommand(u, d), "Listing roles defined at Dataverse " + dvIdtf) ){
                jab.add( json(r) );
            }
            return okResponse(jab);
        } catch (WrappedResponse ex) {
            return ex.getResponse();
        }
	}
    
	@POST
	@Path("{identifier}/roles")
	public Response createRole( RoleDTO roleDto, @PathParam("identifier") String dvIdtf, @QueryParam("key") String apiKey ) {
		try {
            User u = findUserOrDie(apiKey);
            Dataverse dataverse = findDataverseOrDie(dvIdtf);
			return okResponse( json(execCommand(new CreateRoleCommand(roleDto.asRole(), u, dataverse), "Create Role")));
        } catch ( WrappedResponse ce ) {
			return ce.getResponse();
		}
	}
	
	@GET
	@Path("{identifier}/assignments")
	public Response listAssignments( @PathParam("identifier") String dvIdtf, @QueryParam("key") String apiKey ) {
		try {
            User u = findUserOrDie(apiKey);
            Dataverse dataverse = findDataverseOrDie(dvIdtf);
			JsonArrayBuilder jab = Json.createArrayBuilder();
			for ( RoleAssignment ra : execCommand(new ListRoleAssignments(u, dataverse), "Role Assignment Listing") ){
				jab.add( json(ra) );
			}
			return okResponse(jab);
			
		} catch (WrappedResponse ex) {
			return ex.getResponse();
		}
	}
	
	@POST
	@Path("{identifier}/assignments")
	public Response createAssignment( RoleAssignmentDTO ra, @PathParam("identifier") String dvIdtf, @QueryParam("key") String apiKey ) {
		
		try {
            User actingUser = findUserOrDie(apiKey);
            Dataverse dataverse = findDataverseOrDie(dvIdtf);

            RoleAssignee assignee = findAssignee(ra.getAssignee());
            if ( assignee==null ) {
                return errorResponse( Status.BAD_REQUEST, "Assignee not found" );
            }

            DataverseRole theRole;
            Dataverse dv = dataverse;
            theRole = null;
            while ( (theRole==null) && (dv!=null) ) {
                for ( DataverseRole aRole : rolesSvc.availableRoles(dv.getId()) ) {
                    if ( aRole.getAlias().equals(ra.getRole()) ) {
                        theRole = aRole;
                        break;
                    }
                }
                dv = dv.getOwner();
            }
            if ( theRole == null ) {
                return errorResponse( Status.BAD_REQUEST, "Can't find role named '" + ra.getRole() + "' in dataverse " + dataverse);
            }

			RoleAssignment roleAssignment = execCommand( new AssignRoleCommand(assignee, theRole, dataverse, actingUser), "Assign role");
			return okResponse(json(roleAssignment));
			
		} catch (WrappedResponse ex) {
			logger.log(Level.WARNING, "Can''t create assignment: {0}", ex.getMessage());
			return ex.getResponse();
		}
	}
	
	@DELETE
	@Path("{identifier}/assignments/{id}")
	public Response deleteAssignment( @PathParam("id") long assignmentId, @PathParam("identifier") String dvIdtf, @QueryParam("key") String apiKey ) {
		RoleAssignment ra = em.find( RoleAssignment.class, assignmentId );
		if ( ra != null ) {
            try {
                User actingUser = findUserOrDie(apiKey);
                findDataverseOrDie(dvIdtf);
                execCommand( new RevokeRoleCommand(ra, actingUser), "revoking role");
                return okResponse("Role " + ra.getRole().getName() 
                                            + " revoked for assignee " + ra.getAssigneeIdentifier()
                                            + " in " + ra.getDefinitionPoint().accept(DvObject.NamePrinter) );
            } catch (WrappedResponse ex) {
                return ex.getResponse();
            }
		} else {
			return errorResponse( Status.NOT_FOUND, "Role assignment " + assignmentId + " not found" );
		}
	}
	
    @POST
    @Path("{identifier}/actions/:publish") 
    public Response publishDataverse( @PathParam("identifier") String dvIdtf, @QueryParam("key") String apiKey ) {
        try {

            Dataverse dv = findDataverseOrDie(dvIdtf);
            AuthenticatedUser u = findUserOrDie(apiKey);
            
            return okResponse( json(execCommand( new PublishDataverseCommand(u, dv), "Publish Dataverse" )) );
            
        } catch (WrappedResponse wr) {
            return wr.getResponse();
        }
    }
    
    private Dataverse findDataverseOrDie( String dvIdtf ) throws WrappedResponse {
        Dataverse dv = findDataverse(dvIdtf);
        if ( dv == null ) {
            throw new WrappedResponse(errorResponse( Response.Status.NOT_FOUND, "Can't find dataverse with identifier='" + dvIdtf + "'"));
        }
        return dv;
    }
    
    @EJB
    ExplicitGroupServiceBean explicitGroupSvc;
    
    @POST
    @Path("{identifier}/groups/") 
    public Response createExplicitGroup( ExplicitGroupDTO dto, @PathParam("identifier") String dvIdtf, @QueryParam("key") String apiKey ) {
        try {
            
            Dataverse dv = findDataverseOrDie(dvIdtf);
            AuthenticatedUser u = findUserOrDie(apiKey);
            
            ExplicitGroupProvider prv = explicitGroupSvc.getProvider();
            ExplicitGroup newGroup = dto.apply(prv.makeGroup());
            
            newGroup = execCommand( new CreateExplicitGroupCommand(u, dv, newGroup), "Create new group");
            
            String groupUri = String.format("%s/groups/%s", dvIdtf, newGroup.getGroupAliasInOwner());
            return createdResponse( groupUri, json(newGroup) );
            
        } catch (WrappedResponse wr) {
            return wr.getResponse();
        }
    }
    
    @GET
    @Path("{identifier}/groups/") 
    public Response listGroups( @PathParam("identifier") String dvIdtf, @QueryParam("key") String apiKey ) {
        try {
            
            Dataverse dv = findDataverseOrDie(dvIdtf);
            AuthenticatedUser u = findUserOrDie(apiKey);
            
            return okResponse( json(execCommand(new ListExplicitGroupsCommand(u, dv), "Listing groups for dataverse " + dvIdtf )));
            
        } catch (WrappedResponse wr) {
            return wr.getResponse();
        }
    }
    
    @GET
    @Path("{identifier}/groups/{aliasInOwner}") 
    public Response getGroupByOwnerAndAliasInOwner( @PathParam("identifier") String dvIdtf,
                                                    @PathParam("aliasInOwner") String grpAliasInOwner, 
                                                    @QueryParam("key") String apiKey )
    {
        try {
            
            ExplicitGroup eg = findExplicitGroupOrDie(findDataverseOrDie(dvIdtf),
                                                      findUserOrDie(apiKey),
                                                      grpAliasInOwner);
            
            return (eg!=null) ? okResponse( json(eg) ) : notFound("Can't find " + grpAliasInOwner + " in dataverse " + dvIdtf);
            
        } catch (WrappedResponse wr) {
            return wr.getResponse();
        }
    }
    
    @GET
    @Path("{identifier}/groups/{aliasInOwner}/users/email/{userEmail}") 
    public Response getGroupUserByEmail(@PathParam("identifier")String dvIdtf,
                                        @PathParam("aliasInOwner")String grpAliasInOwner,
                                        @PathParam("userEmail")String userEmail,
                                        @QueryParam("key") String apiKey){
        try{
            ExplicitGroup eg = findExplicitGroupOrDie(findDataverseOrDie(dvIdtf),
                                                      findUserOrDie(apiKey),
                                                      grpAliasInOwner);
            if(eg == null)
                return notFound("Can't find " + grpAliasInOwner + " in dataverse " + dvIdtf);
            AuthenticatedUser user = authSvc.getAuthenticatedUserByEmail(userEmail);
            if(user == null)
                return notFound("Can't find user by email" + userEmail);
            if(eg.contains(user)){
                return okResponse(json(user));
            }else{
                return notFound("Can't find user in group " + grpAliasInOwner);
            }
        }catch (WrappedResponse wr) {
            return wr.getResponse();
        }
    }
    
    @PUT
    @Path("{identifier}/groups/{aliasInOwner}") 
    public Response updateGroup(ExplicitGroupDTO groupDto, 
                                @PathParam("identifier") String dvIdtf,
                                @PathParam("aliasInOwner") String grpAliasInOwner, 
                                @QueryParam("key") String apiKey )
    {
        try {
            final AuthenticatedUser user = findUserOrDie(apiKey);
            return okResponse( 
                    json(
                      execCommand( 
                             new UpdateExplicitGroupCommand(user,
                                     groupDto.apply( findExplicitGroupOrDie(findDataverseOrDie(dvIdtf), user, grpAliasInOwner))),
                                                   "Updating group " + dvIdtf + "/" + grpAliasInOwner)));
        } catch (WrappedResponse wr) {
            return wr.getResponse();
        }
    }
    
    @DELETE
    @Path("{identifier}/groups/{aliasInOwner}") 
    public Response deleteGroup(@PathParam("identifier") String dvIdtf,
                                @PathParam("aliasInOwner") String grpAliasInOwner, 
                                @QueryParam("key") String apiKey )
    {
        try {
            final AuthenticatedUser user = findUserOrDie(apiKey);
            execCommand( new DeleteExplicitGroupCommand(user,
                                findExplicitGroupOrDie(findDataverseOrDie(dvIdtf), user, grpAliasInOwner)),
                        "Deleting group " + dvIdtf + "/" + grpAliasInOwner );
            
            return okResponse( "Group " + dvIdtf + "/" + grpAliasInOwner + " deleted" );
        } catch (WrappedResponse wr) {
            return wr.getResponse();
        }
    }
    
    @POST
    @Path("{identifier}/groups/{aliasInOwner}/roleAssignees") 
    public Response addRoleAssingees(List<String> roleAssingeeIdentifiers, 
                                @PathParam("identifier") String dvIdtf,
                                @PathParam("aliasInOwner") String grpAliasInOwner, 
                                @QueryParam("key") String apiKey )
    {
        try {
            final AuthenticatedUser user = findUserOrDie(apiKey);
            return okResponse( 
                    json(
                      execCommand( 
                              new AddRoleAssigneesToExplicitGroupCommand(user, 
                                      findExplicitGroupOrDie(findDataverseOrDie(dvIdtf), user, grpAliasInOwner),
                                      new TreeSet<>(roleAssingeeIdentifiers)),
                              "Adding role assignees to group " + dvIdtf + "/" + grpAliasInOwner )));
        } catch (WrappedResponse wr) {
            return wr.getResponse();
        }
    }
    
    @PUT
    @Path("{identifier}/groups/{aliasInOwner}/roleAssignees/{roleAssigneeIdentifier: .*}") 
    public Response addRoleAssingee( @PathParam("identifier") String dvIdtf,
                                     @PathParam("aliasInOwner") String grpAliasInOwner, 
                                     @PathParam("roleAssigneeIdentifier") String roleAssigneeIdentifier, 
                                     @QueryParam("key") String apiKey ) {
        return addRoleAssingees(Collections.singletonList(roleAssigneeIdentifier), dvIdtf, grpAliasInOwner, apiKey);
    }
    
    @DELETE
    @Path("{identifier}/groups/{aliasInOwner}/roleAssignees/{roleAssigneeIdentifier: .*}") 
    public Response deleteRoleAssingee( @PathParam("identifier") String dvIdtf,
                                        @PathParam("aliasInOwner") String grpAliasInOwner, 
                                        @PathParam("roleAssigneeIdentifier") String roleAssigneeIdentifier, 
                                        @QueryParam("key") String apiKey ) {
        
        try {
            final AuthenticatedUser user = findUserOrDie(apiKey);
            return okResponse( 
                    json(
                      execCommand( 
                              new RemoveRoleAssigneesFromExplicitGroupCommand(user, 
                                      findExplicitGroupOrDie(findDataverseOrDie(dvIdtf), user, grpAliasInOwner),
                                      Collections.singleton(roleAssigneeIdentifier)),
                              "Adding role assignees to group " + dvIdtf + "/" + grpAliasInOwner )));
        } catch (WrappedResponse wr) {
            return wr.getResponse();
        }
    }
    
    private ExplicitGroup findExplicitGroupOrDie( DvObject dv, User u, String groupIdtf ) throws WrappedResponse {
        ExplicitGroup eg = execCommand(new GetExplicitGroupCommand(u, dv, groupIdtf ),
                                            "Listing groups for dataverse " + dv.getId() );
        if ( eg == null ) throw new WrappedResponse( notFound("Can't find " + groupIdtf + " in dataverse " + dv.getId()));
        return eg;
    }

    @GET
    @Path("{identifier}/links")
    public Response listLinks(@PathParam("identifier") String dvIdtf, @QueryParam("key") String apiKey) {
        try {

            Dataverse dv = findDataverseOrDie(dvIdtf);
            AuthenticatedUser u = findUserOrDie(apiKey);
            if (!u.isSuperuser()) {
                return errorResponse(Status.FORBIDDEN, "Not a superuser");
            }

            List<Dataverse> dvsThisDvHasLinkedToList = dataverseSvc.findDataversesThisIdHasLinkedTo(dv.getId());
            JsonArrayBuilder dvsThisDvHasLinkedToBuilder = Json.createArrayBuilder();
            for (Dataverse dataverse : dvsThisDvHasLinkedToList) {
                dvsThisDvHasLinkedToBuilder.add(dataverse.getAlias());
            }

            List<Dataverse> dvsThatLinkToThisDvList = dataverseSvc.findDataversesThatLinkToThisDvId(dv.getId());
            JsonArrayBuilder dvsThatLinkToThisDvBuilder = Json.createArrayBuilder();
            for (Dataverse dataverse : dvsThatLinkToThisDvList) {
                dvsThatLinkToThisDvBuilder.add(dataverse.getAlias());
            }

            List<Dataset> datasetsThisDvHasLinkedToList = dataverseSvc.findDatasetsThisIdHasLinkedTo(dv.getId());
            JsonArrayBuilder datasetsThisDvHasLinkedToBuilder = Json.createArrayBuilder();
            for (Dataset dataset : datasetsThisDvHasLinkedToList) {
                datasetsThisDvHasLinkedToBuilder.add(dataset.getLatestVersion().getTitle());
            }

            JsonObjectBuilder response = Json.createObjectBuilder();
            response.add("dataverses that the " + dv.getAlias() + " dataverse has linked to", dvsThisDvHasLinkedToBuilder);
            response.add("dataverses that link to the " + dv.getAlias(), dvsThatLinkToThisDvBuilder);
            response.add("datasets that the " + dv.getAlias() + " has linked to", datasetsThisDvHasLinkedToBuilder);
            return okResponse(response);

        } catch (WrappedResponse wr) {
            return wr.getResponse();
        }
    }

}
