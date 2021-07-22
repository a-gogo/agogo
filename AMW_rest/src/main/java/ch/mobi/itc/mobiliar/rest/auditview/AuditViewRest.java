package ch.mobi.itc.mobiliar.rest.auditview;

import ch.mobi.itc.mobiliar.rest.dtos.AuditViewEntryDTO;
import ch.puzzle.itc.mobiliar.business.auditview.boundary.AuditViewBoundary;
import ch.puzzle.itc.mobiliar.business.property.boundary.PropertyEditor;
import ch.puzzle.itc.mobiliar.business.auditview.entity.AuditViewEntry;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@Stateless
@Path("/auditview")
@Api(value = "/auditview", description = "Managing auditview")
public class AuditViewRest {

    @Inject
    AuditViewBoundary auditViewBoundary;

    @Inject
    PropertyEditor propertyEditor;

    @GET
    @Path("/resource/{id : \\d+}")
    @ApiOperation(value = "Get detail information of a Deployment - used by Angular")
    public Response getAuditLog(@ApiParam("resource ID") @PathParam("id") Integer resourceId,
                                @QueryParam("contextId") Integer contextId) {
        List<AuditViewEntry> auditlogForResource = new ArrayList<>();
//        List<ResourceEditProperty> propertiesForResourceIncludingDescriptors = propertyEditor.getPropertiesForResource(resourceId, contextId);
//        List<ResourceEditProperty> propertyDescriptors = filterPropertyDescriptors(propertiesForResourceIncludingDescriptors);
//        List<ResourceEditProperty> propertiesForResource = removePropertyDescriptors(propertiesForResourceIncludingDescriptors, propertyDescriptors);

//        auditlogForResource.addAll(auditViewBoundary.getAuditlogForProperties(propertiesForResource));
//        auditlogForResource.addAll(auditViewBoundary.getAuditlogForPropertyDescriptors(propertyDescriptors));

        auditlogForResource.addAll(auditViewBoundary.getAuditlogForResource(resourceId));

        List<AuditViewEntryDTO> dtos = createDtosAndSortByTimestamp(auditlogForResource);
        return Response.status(Response.Status.OK).entity(dtos).build();
    }

    private void sortByTimestamp(List<AuditViewEntryDTO> dtos) {
        Collections.sort(dtos, new Comparator<AuditViewEntryDTO>() {
            @Override
            public int compare(AuditViewEntryDTO o1, AuditViewEntryDTO o2) {
                return o1.getTimestamp().compareTo(o2.getTimestamp());
            }
        });
    }

    private List<AuditViewEntryDTO> createDtosAndSortByTimestamp(List<AuditViewEntry> auditlogForResource) {
        List<AuditViewEntryDTO> dtos = new ArrayList<>(auditlogForResource.size());
        for (AuditViewEntry auditViewEntry : auditlogForResource) {
            dtos.add(new AuditViewEntryDTO(auditViewEntry));
        }
        sortByTimestamp(dtos);
        return dtos;
    }

}
