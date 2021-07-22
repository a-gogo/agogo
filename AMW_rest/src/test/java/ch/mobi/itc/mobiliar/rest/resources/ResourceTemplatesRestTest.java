package ch.mobi.itc.mobiliar.rest.resources;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.ArrayList;

import javax.ws.rs.core.Response;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ch.mobi.itc.mobiliar.rest.dtos.TemplateDTO;
import ch.puzzle.itc.mobiliar.business.resourcegroup.boundary.ResourceLocator;
import ch.puzzle.itc.mobiliar.business.template.boundary.TemplateEditor;
import ch.puzzle.itc.mobiliar.business.template.control.TemplatesScreenDomainService;
import ch.puzzle.itc.mobiliar.business.template.entity.TemplateDescriptorEntity;
import ch.puzzle.itc.mobiliar.common.exception.AMWException;
import ch.puzzle.itc.mobiliar.common.exception.NotFoundException;
import ch.puzzle.itc.mobiliar.common.exception.ValidationException;

public class ResourceTemplatesRestTest {

    @InjectMocks
    private ResourceTemplatesRest resourceTemplatesRest;
    
    @Mock
    private ResourceLocator resourceLocator;

    @Mock
    private TemplateEditor templateEditor;

    @Mock
    private TemplatesScreenDomainService templateService;

    @Before
    public void configure() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void idShouldBeResetOnCreateResourceTemplates() throws AMWException {
        TemplateDTO templateDTO = new TemplateDTO();
        templateDTO.setId(123);
        templateDTO.setName("test");

        Response response = resourceTemplatesRest.createResourceTemplates("resourceGroupName", "releaseName", templateDTO);
        
        TemplateDTO result = (TemplateDTO) response.getEntity();
        assertNull(result.getId());
        assertEquals("test", result.getName());
    }

    @Test(expected = NotFoundException.class)
    public void shouldRaisNotFoundExceptionOnGetResourceTemplate() throws ValidationException, NotFoundException {
        resourceTemplatesRest.getResourceTemplate("resourceGroupName", "releaseName", "templateName");
    }

    @Test
    public void shouldReturnCorrectTemplateOnOnGetResourceTemplate() throws ValidationException, NotFoundException {
        ArrayList<TemplateDescriptorEntity> templates = new ArrayList<>();
        TemplateDescriptorEntity template = new TemplateDescriptorEntity();
        template.setName("template1");
        templates.add(template);
        template = new TemplateDescriptorEntity();
        template.setName("template2");
        templates.add(template);

        when(templateService.getGlobalTemplateDescriptorsForResource(eq("resourceGroupName"), eq("releaseName"), eq(false))).thenReturn(templates);

        TemplateDTO templateDTO = resourceTemplatesRest.getResourceTemplate("resourceGroupName", "releaseName", "template2");
    
        assertEquals("template2", templateDTO.getName());
    }
    
}
