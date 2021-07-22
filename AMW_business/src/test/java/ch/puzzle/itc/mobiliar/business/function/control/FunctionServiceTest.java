/*
 * AMW - Automated Middleware allows you to manage the configurations of
 * your Java EE applications on an unlimited number of different environments
 * with various versions, including the automated deployment of those apps.
 * Copyright (C) 2013-2016 by Puzzle ITC
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package ch.puzzle.itc.mobiliar.business.function.control;

import static org.junit.Assert.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.*;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import ch.puzzle.itc.mobiliar.builders.ResourceEntityBuilder;
import ch.puzzle.itc.mobiliar.builders.ResourceTypeEntityBuilder;
import ch.puzzle.itc.mobiliar.business.function.entity.AmwFunctionEntity;
import ch.puzzle.itc.mobiliar.business.function.entity.AmwFunctionEntityBuilder;
import ch.puzzle.itc.mobiliar.business.property.entity.MikEntity;
import ch.puzzle.itc.mobiliar.business.resourcegroup.control.ResourceRepository;
import ch.puzzle.itc.mobiliar.business.resourcegroup.entity.ResourceEntity;
import ch.puzzle.itc.mobiliar.business.resourcegroup.entity.ResourceTypeEntity;

@RunWith(MockitoJUnitRunner.class)
public class FunctionServiceTest {

	private static final String FUNCTION_NAME_A = "functionNameA";
	private static final String FUNCTION_NAME_B = "functionNameB";
	private static final String FUNCTION_NAME_C = "functionNameC";
	private static final String FUNCTION_NAME_D = "functionNameD";
	private static final String FUNCTION_NAME_E = "functionNameE";
	private static final String FUNCTION_NAME_F = "functionNameF";

	private static final Integer FUNCTION_ID_1 = 1;
	private static final Integer FUNCTION_ID_2 = 2;
	private static final Integer FUNCTION_ID_3 = 3;
	private static final Integer FUNCTION_ID_4 = 4;
	private static final Integer FUNCTION_ID_5 = 5;
	private static final Integer FUNCTION_ID_6 = 6;
	private static final Integer FUNCTION_ID_7 = 7;
	private static final Integer FUNCTION_ID_8 = 8;
	private static final Integer FUNCTION_ID_9 = 9;
	private static final Integer FUNCTION_ID_10 = 10;
    private static final Integer FUNCTION_ID_11 = 11;

    private final AmwFunctionEntity FUNCTION_A = createFunction(FUNCTION_NAME_A, FUNCTION_ID_1);
    private final AmwFunctionEntity FUNCTION_A_OVERWRITE = createOverwritingFunction(FUNCTION_A, FUNCTION_ID_2);
    private final AmwFunctionEntity FUNCTION_A_OVERWRITE_2 = createOverwritingFunction(FUNCTION_A_OVERWRITE, FUNCTION_ID_6);

    private final AmwFunctionEntity OTHER_FUNCTION_A = createFunction(FUNCTION_NAME_A, FUNCTION_ID_11);

    private final AmwFunctionEntity FUNCTION_B = createFunction(FUNCTION_NAME_B, FUNCTION_ID_3);
	private final AmwFunctionEntity FUNCTION_B_OVERWRITE = createOverwritingFunction(FUNCTION_B, FUNCTION_ID_4);

	private final AmwFunctionEntity FUNCTION_C = createFunction(FUNCTION_NAME_C, FUNCTION_ID_5);
	private final AmwFunctionEntity FUNCTION_C_OVERWRITE = createOverwritingFunction(FUNCTION_C, FUNCTION_ID_7);

	private final AmwFunctionEntity FUNCTION_D = createFunction(FUNCTION_NAME_D, FUNCTION_ID_8);

    private final AmwFunctionEntity FUNCTION_E = createFunction(FUNCTION_NAME_E, FUNCTION_ID_9);

    private final AmwFunctionEntity FUNCTION_F = createFunction(FUNCTION_NAME_F, FUNCTION_ID_10);

    private ResourceTypeEntity rootResourceType = createRootResourceType();


    @Mock
	FunctionRepository functionRepositoryMock;

	@Mock
	private ResourceRepository resourceRepositoryMock;

	@InjectMocks
	private FunctionService functionService;


	@Before
	public void setUp(){
		when(functionRepositoryMock.getFunctionByIdWithMiksAndParentChildFunctions(FUNCTION_A.getId())).thenReturn(FUNCTION_A);
		when(functionRepositoryMock.getFunctionByIdWithMiksAndParentChildFunctions(FUNCTION_A_OVERWRITE.getId())).thenReturn(FUNCTION_A_OVERWRITE);
        when(functionRepositoryMock.getFunctionByIdWithMiksAndParentChildFunctions(OTHER_FUNCTION_A.getId())).thenReturn(OTHER_FUNCTION_A);
		when(functionRepositoryMock.getFunctionByIdWithMiksAndParentChildFunctions(FUNCTION_B.getId())).thenReturn(FUNCTION_B);
		when(functionRepositoryMock.getFunctionByIdWithMiksAndParentChildFunctions(FUNCTION_B_OVERWRITE.getId())).thenReturn(FUNCTION_B_OVERWRITE);
		when(functionRepositoryMock.getFunctionByIdWithMiksAndParentChildFunctions(FUNCTION_C.getId())).thenReturn(FUNCTION_C);
		when(functionRepositoryMock.getFunctionByIdWithMiksAndParentChildFunctions(FUNCTION_D.getId())).thenReturn(FUNCTION_D);
		when(functionRepositoryMock.getFunctionByIdWithMiksAndParentChildFunctions(FUNCTION_E.getId())).thenReturn(FUNCTION_E);

	}


	@Test
	public void getAllOverwritableSupertypeFunctionsForResourceShouldNotBeNull() {
		// given
		ResourceEntity resource = createResourceWithType("resourceName", 1, createRootResourceType());

		// when
		List<AmwFunctionEntity> resourceFunctions = functionService.getAllOverwritableSupertypeFunctions(resource);

		// then
		assertNotNull(resourceFunctions);
	}


	@Test
	public void getAllOverwritableSupertypeFunctionsForResourceWithoutSupertypeFunctionsShouldReturnEmptyList() {
		// given
		ResourceEntity resource = createResourceWithType("resourceName", 1, createRootResourceType());

		// when
		List<AmwFunctionEntity> resourceSuperTypeFunctions = functionService.getAllOverwritableSupertypeFunctions(resource);

		// then
		assertTrue(resourceSuperTypeFunctions.isEmpty());
	}

	@Test
	public void getAllOverwritableSupertypeFunctionsForResourceWithRootSupertypeFunctionsNotOverwrittenShouldReturnAllSupertypeFunctions() {
		// given

		ResourceEntity resource = createResourceWithType("resourceName", 1, createRootResourceType(FUNCTION_A, FUNCTION_B));

		// when
		List<AmwFunctionEntity> resourceSuperTypeFunctions = functionService.getAllOverwritableSupertypeFunctions(resource);

		// then
		assertTrue(resourceSuperTypeFunctions.contains(FUNCTION_A));
		assertTrue(resourceSuperTypeFunctions.contains(FUNCTION_B));
	}

	@Test
	public void getAllOverwritableSupertypeFunctionsForResourceWithSupertypeFunctionsButPartiallyOverwrittenWithSameFunctionNameShouldReturnNotOverwrittenSupertypeFunctions() {
		// given
		ResourceTypeEntity rootResourceType = createRootResourceType(FUNCTION_A, FUNCTION_B);
		ResourceEntity resource = createResourceWithType("resourceName", 1, rootResourceType, FUNCTION_B_OVERWRITE);

		// when
		List<AmwFunctionEntity> resourceSuperTypeFunctions = functionService.getAllOverwritableSupertypeFunctions(resource);

		// then
		assertTrue(resourceSuperTypeFunctions.contains(FUNCTION_A));
		assertFalse(resourceSuperTypeFunctions.contains(FUNCTION_B));
	}

	@Test
	public void getAllOverwritableSupertypeFunctionsForResourceWithSupertypeFunctionsButOverwrittenInSubTypeShouldReturnAllSupertypeFunctions() {
		// given

		ResourceTypeEntity rootResourceType = createRootResourceType(FUNCTION_A, FUNCTION_B);
		ResourceTypeEntity subResourceType = createSubResourceType(rootResourceType, FUNCTION_A_OVERWRITE, FUNCTION_B);

		ResourceEntity resource = createResourceWithType("resourceName", 1, subResourceType);

		// when
		List<AmwFunctionEntity> resourceSuperTypeFunctions = functionService.getAllOverwritableSupertypeFunctions(resource);

		// then
		assertTrue(resourceSuperTypeFunctions.contains(FUNCTION_A_OVERWRITE));
		assertTrue(resourceSuperTypeFunctions.contains(FUNCTION_B));
	}

	@Test
	public void getAllOverwritableSupertypeFunctionsForResourceWithSupertypeFunctionsButOverwrittenInSubTypeByFunctionsWithSameNameShouldReturnAllInSubTypeOverwritingFunctions() {
		// given

		ResourceTypeEntity rootResourceType = createRootResourceType(FUNCTION_A, FUNCTION_B);
		ResourceTypeEntity subResourceType = createSubResourceType(rootResourceType, FUNCTION_A_OVERWRITE, FUNCTION_B_OVERWRITE);

		ResourceEntity resource = createResourceWithType("resourceName", 1, subResourceType);

		// when
		List<AmwFunctionEntity> resourceSuperTypeFunctions = functionService.getAllOverwritableSupertypeFunctions(resource);

		// then
		assertTrue(resourceSuperTypeFunctions.contains(FUNCTION_A_OVERWRITE));
		assertTrue(resourceSuperTypeFunctions.contains(FUNCTION_B_OVERWRITE));

		assertFalse(resourceSuperTypeFunctions.contains(FUNCTION_A));
		assertFalse(resourceSuperTypeFunctions.contains(FUNCTION_B));
	}

	@Test
	public void getAllOverwritableSupertypeFunctionsForResourceWithSupertypeFunctionsNotOverwrittenShouldReturnAllSupertypeFunctions() {
		// given

		ResourceTypeEntity rootResourceType = createRootResourceType(FUNCTION_A, FUNCTION_B);
		ResourceTypeEntity subResourceType = createSubResourceType(rootResourceType, FUNCTION_C);

		ResourceEntity resource = createResourceWithType("resourceName", 1, subResourceType);

		// when
		List<AmwFunctionEntity> resourceSuperTypeFunctions = functionService.getAllOverwritableSupertypeFunctions(resource);

		// then
		assertTrue(resourceSuperTypeFunctions.contains(FUNCTION_A));
		assertTrue(resourceSuperTypeFunctions.contains(FUNCTION_B));
		assertTrue(resourceSuperTypeFunctions.contains(FUNCTION_C));
	}

	@Test
	public void getAllOverwritableSupertypeFunctionsForResourceWithSupertypeFunctionsButOverwrittenInResourceShouldReturnNotOverwrittenSupertypeFunctions() {
		// given
		ResourceTypeEntity rootResourceType = createRootResourceType(FUNCTION_A, FUNCTION_B);
		ResourceTypeEntity subResourceType = createSubResourceType(rootResourceType, FUNCTION_B_OVERWRITE);

		ResourceEntity resource = createResourceWithType("resourceName", 1, subResourceType, FUNCTION_A_OVERWRITE);

		// when
		List<AmwFunctionEntity> resourceSuperTypeFunctions = functionService.getAllOverwritableSupertypeFunctions(resource);

		// then
		assertTrue(resourceSuperTypeFunctions.contains(FUNCTION_B_OVERWRITE));

		assertFalse(resourceSuperTypeFunctions.contains(FUNCTION_A));
		assertFalse(resourceSuperTypeFunctions.contains(FUNCTION_B));
		assertFalse(resourceSuperTypeFunctions.contains(FUNCTION_A_OVERWRITE));
	}

    @Test
    public void getAllOverwritableSupertypeFunctionsForResourceWhereRootResourceTypeHasMultipleSubResourceTypesShouldGetOnlyRelatedParentFunctions() {
        // given
        ResourceTypeEntity rootResourceType = createRootResourceType(FUNCTION_A, FUNCTION_B);

        ResourceTypeEntity subResourceType1 = createSubResourceType(rootResourceType, FUNCTION_C);
        ResourceEntity resource1 = createResourceWithType("resourceName", 1, subResourceType1);

        ResourceTypeEntity otherSubResourceType = createSubResourceType(rootResourceType, FUNCTION_D);
        ResourceEntity otherResource = createResourceWithType("otherResourceName", 2, otherSubResourceType);

        // when
        List<AmwFunctionEntity> resourceSuperTypeFunctions = functionService.getAllOverwritableSupertypeFunctions(resource1);

        // then
        assertTrue(resourceSuperTypeFunctions.contains(FUNCTION_A));
        assertTrue(resourceSuperTypeFunctions.contains(FUNCTION_B));
        assertTrue(resourceSuperTypeFunctions.contains(FUNCTION_C));

        // when
        List<AmwFunctionEntity> otherResourceSuperTypeFunctions = functionService.getAllOverwritableSupertypeFunctions(otherResource);

        // then
        assertTrue(otherResourceSuperTypeFunctions.contains(FUNCTION_A));
        assertTrue(otherResourceSuperTypeFunctions.contains(FUNCTION_B));
        assertTrue(otherResourceSuperTypeFunctions.contains(FUNCTION_D));
    }


	@Test
	public void getAllOverwritableSupertypeFunctionsForResourceTypeWithoutSupertypeFunctionsShouldReturnEmptyList() {
		// given
		ResourceTypeEntity rootResourceType = createRootResourceType();
		ResourceTypeEntity subResourceType = createSubResourceType(rootResourceType);

		// when
		List<AmwFunctionEntity> resourceSuperTypeFunctions = functionService.getAllOverwritableSupertypeFunctions(subResourceType);

		// then
		assertTrue(resourceSuperTypeFunctions.isEmpty());
	}

	@Test
	public void getAllOverwritableSupertypeFunctionsForResourceTypeWithRootSupertypeFunctionsNotOverwrittenShouldReturnAllSupertypeFunctions() {
		// given

		ResourceTypeEntity rootResourceType = createRootResourceType(FUNCTION_A, FUNCTION_B);
		ResourceTypeEntity subResourceType = createSubResourceType(rootResourceType);

		// when
		List<AmwFunctionEntity> resourceTypeSuperTypeFunctions = functionService.getAllOverwritableSupertypeFunctions(subResourceType);

		// then
		assertTrue(resourceTypeSuperTypeFunctions.contains(FUNCTION_A));
		assertTrue(resourceTypeSuperTypeFunctions.contains(FUNCTION_B));
	}

	@Test
	public void getAllOverwritableSupertypeFunctionsForResourceTypeWithSupertypeFunctionsButPartiallyOverwrittenShouldReturnNotOverwrittenSupertypeFunctions() {
		// given

		ResourceTypeEntity rootResourceType = createRootResourceType(FUNCTION_A, FUNCTION_B);
		ResourceTypeEntity subResourceType = createSubResourceType(rootResourceType, FUNCTION_B_OVERWRITE);

		// when
		List<AmwFunctionEntity> resourceTypeSuperTypeFunctions = functionService.getAllOverwritableSupertypeFunctions(subResourceType);

		// then
		assertTrue(resourceTypeSuperTypeFunctions.contains(FUNCTION_A));
		assertFalse(resourceTypeSuperTypeFunctions.contains(FUNCTION_B));
		assertFalse(resourceTypeSuperTypeFunctions.contains(FUNCTION_B_OVERWRITE));
	}

	@Test
	public void getAllOverwritableSupertypeFunctionsForResourceTypeWithSupertypeFunctionsButOverwrittenInSubTypeShouldReturnAllSupertypeFunctions() {
		// given

		ResourceTypeEntity rootResourceType = createRootResourceType(FUNCTION_A, FUNCTION_B);
		ResourceTypeEntity subResourceType = createSubResourceType(rootResourceType, FUNCTION_A, FUNCTION_B);
		ResourceTypeEntity subSubResourceType = createSubResourceType(subResourceType);

		// when
		List<AmwFunctionEntity> resourceTypeSuperTypeFunctions = functionService.getAllOverwritableSupertypeFunctions(subSubResourceType);

		// then
		assertTrue(resourceTypeSuperTypeFunctions.contains(FUNCTION_A));
		assertTrue(resourceTypeSuperTypeFunctions.contains(FUNCTION_B));
	}

	@Test
	public void getAllOverwritableSupertypeFunctionsForResourceTypeWithSupertypeFunctionsButOverwrittenInSubTypeWithSameFunctionNameShouldReturnAllOverwritingFunctions() {
		// given

		ResourceTypeEntity rootResourceType = createRootResourceType(FUNCTION_A, FUNCTION_B);
		ResourceTypeEntity subResourceType = createSubResourceType(rootResourceType, FUNCTION_A_OVERWRITE, FUNCTION_B_OVERWRITE);
		ResourceTypeEntity subSubResourceType = createSubResourceType(subResourceType);

		// when
		List<AmwFunctionEntity> resourceTypeSuperTypeFunctions = functionService.getAllOverwritableSupertypeFunctions(subSubResourceType);

		// then
		assertTrue(resourceTypeSuperTypeFunctions.contains(FUNCTION_A_OVERWRITE));
		assertTrue(resourceTypeSuperTypeFunctions.contains(FUNCTION_B_OVERWRITE));

		assertFalse(resourceTypeSuperTypeFunctions.contains(FUNCTION_A));
		assertFalse(resourceTypeSuperTypeFunctions.contains(FUNCTION_B));
	}

	@Test
	public void getAllOverwritableSupertypeFunctionsForResourceTypeWithSupertypeFunctionsNotOverwrittenShouldReturnAllSupertypeFunctions() {
		// given

		ResourceTypeEntity rootResourceType = createRootResourceType(FUNCTION_A, FUNCTION_B);
		ResourceTypeEntity subResourceType = createSubResourceType(rootResourceType, FUNCTION_C);

		ResourceTypeEntity subSubResourceType = createSubResourceType(subResourceType);

		// when
		List<AmwFunctionEntity> resourceTypeSuperTypeFunctions = functionService.getAllOverwritableSupertypeFunctions(subSubResourceType);

		// then
		assertTrue(resourceTypeSuperTypeFunctions.contains(FUNCTION_A));
		assertTrue(resourceTypeSuperTypeFunctions.contains(FUNCTION_B));
		assertTrue(resourceTypeSuperTypeFunctions.contains(FUNCTION_C));
	}


	@Test
	public void testGetAllFunctionsForResource() throws Exception {
		// given
		ResourceTypeEntity rootResourceType = createRootResourceType(FUNCTION_A, FUNCTION_B, FUNCTION_C, FUNCTION_D);
		ResourceTypeEntity subResourceType = createSubResourceType(rootResourceType, FUNCTION_A_OVERWRITE, FUNCTION_B_OVERWRITE, FUNCTION_E);
		ResourceEntity resource = createResourceWithType("amw", 1000, subResourceType, FUNCTION_A_OVERWRITE_2, FUNCTION_C_OVERWRITE, FUNCTION_F);

		// when(resourceTypeRepositoryMock.loadWithFunctionsAndMiksForId(subResourceType.getId())).thenReturn(subResourceType);
		when(resourceRepositoryMock.loadWithFunctionsAndMiksForId(resource.getId())).thenReturn(resource);

		// when
		List<AmwFunctionEntity> result = functionService.getAllFunctionsForResource(resource);

		// then
		assertEquals(6, result.size());

		assertTrue(result.contains(FUNCTION_A_OVERWRITE_2));
		assertTrue(result.contains(FUNCTION_C_OVERWRITE));
		assertTrue(result.contains(FUNCTION_F));

		assertFalse(result.contains(FUNCTION_A_OVERWRITE)); // overwritten by resource instance
		assertTrue(result.contains(FUNCTION_B_OVERWRITE));
		assertTrue(result.contains(FUNCTION_E));

		// functions from type not overwritten by instance or subtype
		assertFalse(result.contains(FUNCTION_A)); // overwritten by subType and resource instance
		assertFalse(result.contains(FUNCTION_B)); // overwritten by subType
		assertFalse(result.contains(FUNCTION_C)); // overwritten by resource instance
		assertTrue(result.contains(FUNCTION_D));
	}


    @Test
    public void saveFunctionWithMiksWhenFunctionHasIdShouldAddMiksAndMerge() {
        // given

        String mikName1 = "mik1";
        String mikName2 = "mik2";
        Set<String> mikNames = new HashSet<>();
        mikNames.add(mikName1);
        mikNames.add(mikName2);

        AmwFunctionEntity functionWithoutMiks = createFunction("functionName", 1);

        // when
        functionService.saveFunctionWithMiks(functionWithoutMiks, mikNames);

        // then

        ArgumentCaptor<AmwFunctionEntity> argCaptor = ArgumentCaptor.forClass(AmwFunctionEntity.class);
        verify(functionRepositoryMock).persistOrMergeFunction(argCaptor.capture());

        AmwFunctionEntity persistedFunction = argCaptor.getValue();
        assertEquals(mikNames.size(), persistedFunction.getMikNames().size());

    }

    @Test
    public void saveFunctionWithMiksWhenFunctionHasNoIdShouldAddMiksAndPersist() {
        // given

        String mikName1 = "mik1";
        String mikName2 = "mik2";
        Set<String> mikNames = new HashSet<>();
        mikNames.add(mikName1);
        mikNames.add(mikName2);

        AmwFunctionEntity functionWithoutMiks = createFunction("functionName", null);

        // when
        functionService.saveFunctionWithMiks(functionWithoutMiks, mikNames);

        // then
        ArgumentCaptor<AmwFunctionEntity> argCaptor = ArgumentCaptor.forClass(AmwFunctionEntity.class);
        verify(functionRepositoryMock).persistOrMergeFunction(argCaptor.capture());

        AmwFunctionEntity persistedFunction = argCaptor.getValue();
        assertEquals(mikNames.size(), persistedFunction.getMikNames().size());
    }

    @Test
    public void findFunctionsByNameInNamespaceForResourceWithoutFunctionsShouldReturnEmptyList() {
        // given

        ResourceTypeEntity rootResourceType = createRootResourceType();

        ResourceTypeEntity subResourceType = createSubResourceType(rootResourceType);
        ResourceEntity resource = createResourceWithType("amw", 1000, subResourceType);

        when(resourceRepositoryMock.loadWithFunctionsAndMiksForId(resource.getId())).thenReturn(resource);

        // when
        List<AmwFunctionEntity> functionsWithName = functionService.findFunctionsByNameInNamespace(resource, FUNCTION_A.getName());

        // then
        assertTrue(functionsWithName.isEmpty());
    }

    @Test
    public void findFunctionsByNameInNamespaceForResourceWithoutFunctionsButSameFunctionNameInOtherSubResourceTypeShouldReturnEmptyList() {
        // given

        ResourceTypeEntity rootResourceType = createRootResourceType();

        ResourceTypeEntity subResourceType = createSubResourceType(rootResourceType);
        ResourceEntity resource = createResourceWithType("amw", 1000, subResourceType);

        createSubResourceType(rootResourceType, OTHER_FUNCTION_A);

        when(resourceRepositoryMock.loadWithFunctionsAndMiksForId(resource.getId())).thenReturn(resource);

        // when
        List<AmwFunctionEntity> functionsWithName = functionService.findFunctionsByNameInNamespace(resource, FUNCTION_A.getName());

        // then
        assertTrue(functionsWithName.isEmpty());
    }

    @Test
    public void findFunctionsByNameInNamespaceForResourceWithOtherNamedFunctionsShouldReturnEmptyList() {
        // given

        ResourceTypeEntity rootResourceType = createRootResourceType();
        ResourceTypeEntity subResourceType = createSubResourceType(rootResourceType);
        ResourceEntity resource = createResourceWithType("amw", 1000, subResourceType, FUNCTION_B);

        when(resourceRepositoryMock.loadWithFunctionsAndMiksForId(resource.getId())).thenReturn(resource);

        // when
        List<AmwFunctionEntity> functionsWithName = functionService.findFunctionsByNameInNamespace(resource, FUNCTION_A.getName());

        // then
        assertTrue(functionsWithName.isEmpty());
    }

    @Test
    public void findFunctionsByNameInNamespaceForResourceWithSameNamedResourceFunctionShouldReturnOtherFunction() {
        // given

        ResourceTypeEntity rootResourceType = createRootResourceType();
        ResourceTypeEntity subResourceType = createSubResourceType(rootResourceType);
        ResourceEntity resource = createResourceWithType("amw", 1000, subResourceType, OTHER_FUNCTION_A);

        when(resourceRepositoryMock.loadWithFunctionsAndMiksForId(resource.getId())).thenReturn(resource);

        // when
        List<AmwFunctionEntity> functionsWithName = functionService.findFunctionsByNameInNamespace(resource, FUNCTION_A.getName());

        // then
        assertTrue(functionsWithName.contains(OTHER_FUNCTION_A));
    }

    @Test
    public void findFunctionsByNameInNamespaceForResourceWithOtherNamedFunctionsOnSubTypeShouldReturnEmptyList() {
        // given

        ResourceTypeEntity rootResourceType = createRootResourceType();
        ResourceTypeEntity subResourceType = createSubResourceType(rootResourceType, FUNCTION_B);
        ResourceEntity resource = createResourceWithType("amw", 1000, subResourceType);

        when(resourceRepositoryMock.loadWithFunctionsAndMiksForId(resource.getId())).thenReturn(resource);

        // when
        List<AmwFunctionEntity> functionsWithName = functionService.findFunctionsByNameInNamespace(resource, FUNCTION_A.getName());

        // then
        assertTrue(functionsWithName.isEmpty());
    }

    @Test
    public void findFunctionsByNameInNamespaceForResourceWithSameNamedFunctionsOnSubTypeShouldReturnTrue() {
        // given

        ResourceTypeEntity rootResourceType = createRootResourceType();
        ResourceTypeEntity subResourceType = createSubResourceType(rootResourceType, OTHER_FUNCTION_A);
        ResourceEntity resource = createResourceWithType("amw", 1000, subResourceType);

        when(resourceRepositoryMock.loadWithFunctionsAndMiksForId(resource.getId())).thenReturn(resource);

        // when
        List<AmwFunctionEntity> functionsWithName = functionService.findFunctionsByNameInNamespace(resource, FUNCTION_A.getName());

        // then
        assertTrue(functionsWithName.contains(OTHER_FUNCTION_A));
    }



    @Test
    public void findFunctionsByNameInNamespaceForResourceWithSameNamedFunctionsOtherResourceNotInNamespaceShouldReturnEmptyList() {
        // given

        ResourceTypeEntity rootResourceType = createRootResourceType();
        ResourceTypeEntity subResourceType = createSubResourceType(rootResourceType);
        ResourceTypeEntity otherSubResourceType = createSubResourceType(rootResourceType);

        ResourceEntity resource = createResourceWithType("amw", 1000, subResourceType);
        createResourceWithType("amw2", 1001, otherSubResourceType, OTHER_FUNCTION_A);

        when(resourceRepositoryMock.loadWithFunctionsAndMiksForId(resource.getId())).thenReturn(resource);

        // when
        List<AmwFunctionEntity> functionsWithName = functionService.findFunctionsByNameInNamespace(resource, FUNCTION_A.getName());

        // then
        assertTrue(functionsWithName.isEmpty());
    }


    @Test
    public void findFunctionsByNameInNamespaceForResourceWithOtherNamedFunctionsOnRootTypeShouldReturnEmptyList() {
        // given

        ResourceTypeEntity rootResourceType = createRootResourceType(FUNCTION_B);
        ResourceTypeEntity subResourceType = createSubResourceType(rootResourceType);
        ResourceEntity resource = createResourceWithType("amw", 1000, subResourceType);

        when(resourceRepositoryMock.loadWithFunctionsAndMiksForId(resource.getId())).thenReturn(resource);

        // when
        List<AmwFunctionEntity> functionsWithName = functionService.findFunctionsByNameInNamespace(resource, FUNCTION_A.getName());

        // then
        assertTrue(functionsWithName.isEmpty());
    }

    @Test
    public void findFunctionsByNameInNamespaceForResourceWithSameNamedFunctionsOnRootTypeShouldReturnOtherFunction() {
        // given

        ResourceTypeEntity rootResourceType = createRootResourceType(OTHER_FUNCTION_A);
        ResourceTypeEntity subResourceType = createSubResourceType(rootResourceType);
        ResourceEntity resource = createResourceWithType("amw", 1000, subResourceType);

        when(resourceRepositoryMock.loadWithFunctionsAndMiksForId(resource.getId())).thenReturn(resource);

        // when
        List<AmwFunctionEntity> functionsWithName = functionService.findFunctionsByNameInNamespace(resource, FUNCTION_A.getName());

        // then
        assertTrue(functionsWithName.contains(OTHER_FUNCTION_A));
    }

    @Test
    public void findFunctionsByNameInNamespaceForSubResourceTypeWithoutFunctionsOnRootTypeAndChildResourceShouldReturnEmptyList() {
        // given

        ResourceTypeEntity rootResourceType = createRootResourceType();
        ResourceTypeEntity subResourceType = createSubResourceType(rootResourceType);
        createResourceWithType("amw", 1000, subResourceType);

        // when
        List<AmwFunctionEntity> functionsWithName = functionService.findFunctionsByNameInNamespace(subResourceType, FUNCTION_A.getName());

        // then
        assertTrue(functionsWithName.isEmpty());
    }

    @Test
    public void findFunctionsByNameInNamespaceForSubResourceTypeWithoutFunctionsOnRootTypeAndChildResourceButInOtherSubResourceTypeShouldReturnEmptyList() {
        // given

        ResourceTypeEntity rootResourceType = createRootResourceType();
        ResourceTypeEntity subResourceType = createSubResourceType(rootResourceType);
        createResourceWithType("amw", 1000, subResourceType);
        createSubResourceType(rootResourceType, OTHER_FUNCTION_A);

        // when
        List<AmwFunctionEntity> functionsWithName = functionService.findFunctionsByNameInNamespace(subResourceType, FUNCTION_A.getName());

        // then
        assertTrue(functionsWithName.isEmpty());
    }

    @Test
    public void findFunctionsByNameInNamespaceForSubResourceTypeWithFunctionsWithSameNameOnSubTypeShouldReturnOtherFunction() {
        // given

        ResourceTypeEntity rootResourceType = createRootResourceType();
        ResourceTypeEntity subResourceType = createSubResourceType(rootResourceType, OTHER_FUNCTION_A);
        createResourceWithType("amw", 1000, subResourceType);

        // when
        List<AmwFunctionEntity> functionsWithName = functionService.findFunctionsByNameInNamespace(subResourceType, FUNCTION_A.getName());

        // then
        assertTrue(functionsWithName.contains(OTHER_FUNCTION_A));
    }

    @Test
    public void findFunctionsByNameInNamespaceForSubResourceTypeWithFunctionsWithSameNameOnRootTypeShouldReturnOtherFunction() {
        // given

        ResourceTypeEntity rootResourceType = createRootResourceType(OTHER_FUNCTION_A);
        ResourceTypeEntity subResourceType = createSubResourceType(rootResourceType);
        createResourceWithType("amw", 1000, subResourceType);

        // when
        List<AmwFunctionEntity> functionsWithName = functionService.findFunctionsByNameInNamespace(subResourceType, FUNCTION_A.getName());

        // then
        assertTrue(functionsWithName.contains(OTHER_FUNCTION_A));
    }

    @Test
    public void findFunctionsByNameInNamespaceForSubResourceTypeWithFunctionsWithSameNameOnResourceOneShouldReturnOtherResource() {
        // given

        ResourceTypeEntity rootResourceType = createRootResourceType();
        ResourceTypeEntity subResourceType = createSubResourceType(rootResourceType);
        createResourceWithType("amw", 1000, subResourceType, OTHER_FUNCTION_A);

        // when
        List<AmwFunctionEntity> functionsWithName = functionService.findFunctionsByNameInNamespace(subResourceType, FUNCTION_A.getName());

        // then
        assertTrue(functionsWithName.contains(OTHER_FUNCTION_A));
    }

    @Test
    public void findFunctionsByNameInNamespaceForRootResourceTypeWithoutFunctionsOnSubTypeAndSubTypResourceShouldReturnEmptyList() {
        // given

        ResourceTypeEntity rootResourceType = createRootResourceType();
        ResourceTypeEntity subResourceType = createSubResourceType(rootResourceType);
        createResourceWithType("amw", 1000, subResourceType);

        // when
        List<AmwFunctionEntity> functionsWithName = functionService.findFunctionsByNameInNamespace(rootResourceType, FUNCTION_A.getName());

        // then
        assertTrue(functionsWithName.isEmpty());
    }

    @Test
    public void findFunctionsByNameInNamespaceForRootResourceTypeWithFunctionsWithSameNameOnRootTypeShouldReturnOtherFunction() {
        // given

        ResourceTypeEntity rootResourceType = createRootResourceType(OTHER_FUNCTION_A);
        ResourceTypeEntity subResourceType = createSubResourceType(rootResourceType);
        createResourceWithType("amw", 1000, subResourceType);
        // when
        List<AmwFunctionEntity> functionsWithName = functionService.findFunctionsByNameInNamespace(rootResourceType, FUNCTION_A.getName());

        // then
        assertTrue(functionsWithName.contains(OTHER_FUNCTION_A));
    }

    @Test
    public void findFunctionsByNameInNamespaceForRootResourceTypeWithFunctionsWithSameNameOnSubTypeShouldReturnOtherFunction() {
        // given

        ResourceTypeEntity rootResourceType = createRootResourceType();
        ResourceTypeEntity subResourceType = createSubResourceType(rootResourceType, OTHER_FUNCTION_A);
        createResourceWithType("amw", 1000, subResourceType);

        // when
        List<AmwFunctionEntity> functionsWithName = functionService.findFunctionsByNameInNamespace(rootResourceType, FUNCTION_A.getName());

        // then
        assertTrue(functionsWithName.contains(OTHER_FUNCTION_A));
    }

    @Test
    public void findFunctionsByNameInNamespaceForRootResourceTypeWithFunctionsWithSameNameOnSubTypResourceShouldReturnOtherFunction() {
        // given

        ResourceTypeEntity rootResourceType = createRootResourceType();
        ResourceTypeEntity subResourceType = createSubResourceType(rootResourceType);
        createResourceWithType("amw", 1000, subResourceType, OTHER_FUNCTION_A);

        // when
        List<AmwFunctionEntity> functionsWithName = functionService.findFunctionsByNameInNamespace(rootResourceType, FUNCTION_A.getName());

        // then
        assertTrue(functionsWithName.contains(OTHER_FUNCTION_A));
    }

	@Test
	public void testGetAMWFunctionForMIK() {
		// given
		String mik1Name = "mik1";
		AmwFunctionEntity fct1 = createFunction("fct1", 1, mik1Name);

		String mik2Name = "mik2";
		String mik3Name = "mik3";
		AmwFunctionEntity fct2 = createFunction("fct2", 2, mik2Name, mik3Name);

		List<AmwFunctionEntity> functions = new ArrayList<>();
		functions.add(fct1);
		functions.add(fct2);

		// when then
		assertEquals(fct1, functionService.getAMWFunctionForMIK(functions, "mik1"));
		assertEquals(fct2, functionService.getAMWFunctionForMIK(functions, "mik2"));
		assertEquals(fct2, functionService.getAMWFunctionForMIK(functions, "mik3"));
		assertNull(functionService.getAMWFunctionForMIK(functions, "mik4"));
	}


    @Test
    public void overwriteResourceFunctionShouldCopyRootName() {
        // given
        String mik1Name = "mik1";
        AmwFunctionEntity rootFunction = createFunctionForResourceType(FUNCTION_NAME_A, FUNCTION_ID_1, rootResourceType, mik1Name);
        ResourceEntity resource = createResourceWithType("resource", 99, rootResourceType);

        // when
        AmwFunctionEntity overwriteFunction = functionService.overwriteResourceFunction("functionBody", rootFunction, resource);

        // then
        assertEquals(rootFunction.getName(), overwriteFunction.getName());
    }

    @Test
    public void overwriteResourceFunctionShouldSetFunctionBody() {
        // given
        String functionBody = "functionBody";
        String mik1Name = "mik1";
        AmwFunctionEntity rootFunction = createFunctionForResourceType(FUNCTION_NAME_A, FUNCTION_ID_1, rootResourceType, mik1Name);
        ResourceEntity resource = createResourceWithType("resource", 99, rootResourceType);

        // when
        AmwFunctionEntity overwriteFunction = functionService.overwriteResourceFunction(functionBody, rootFunction, resource);

        // then
        assertEquals(functionBody, overwriteFunction.getImplementation());
    }

    @Test
    public void overwriteResourceFunctionShouldCopyRootMiks() {
        // given
        String mik1Name = "mik1";
        AmwFunctionEntity rootFunction = createFunctionForResourceType(FUNCTION_NAME_A, FUNCTION_ID_1, rootResourceType, mik1Name);
        ResourceEntity resource = createResourceWithType("resource", 99, rootResourceType);

        // when
        AmwFunctionEntity overwriteFunction = functionService.overwriteResourceFunction("functionBody", rootFunction, resource);

        // then
        assertEquals(rootFunction.getMikNames().size(), overwriteFunction.getMikNames().size());
        assertEquals(rootFunction.getCommaseparatedMikNames(), overwriteFunction.getCommaseparatedMikNames());
    }

    @Test
    public void overwriteResourceFunctionShouldNotSetId() {
        // given
        String mik1Name = "mik1";
        AmwFunctionEntity rootFunction = createFunctionForResourceType(FUNCTION_NAME_A, FUNCTION_ID_1, rootResourceType, mik1Name);
        ResourceEntity resource = createResourceWithType("resource", 99, rootResourceType);

        // when
        AmwFunctionEntity overwriteFunction = functionService.overwriteResourceFunction("functionBody", rootFunction, resource);

        // then
        assertNotEquals(FUNCTION_ID_1, overwriteFunction.getId());
    }

    @Test
    public void overwriteResourceFunctionShouldSetResourceToOverwritingFunction() {
        // given
        String mik1Name = "mik1";
        AmwFunctionEntity rootFunction = createFunctionForResourceType(FUNCTION_NAME_A, FUNCTION_ID_1, rootResourceType, mik1Name);
        ResourceEntity resource = createResourceWithType("resource", 99, rootResourceType);

        // when
        AmwFunctionEntity overwriteFunction = functionService.overwriteResourceFunction("functionBody", rootFunction, resource);

        // then
        assertEquals(resource, overwriteFunction.getResource());
    }

    @Test
    public void overwriteResourceFunctionShouldSetParentAndChildFunction() {
        // given
        AmwFunctionEntity rootFunction = createFunctionForResourceType(FUNCTION_NAME_A, FUNCTION_ID_1, rootResourceType, "mik1Name");
        ResourceEntity resource = createResourceWithType("resource", 99, rootResourceType);
        assertFalse(rootFunction.isOverwrittenBySubTypeOrResourceFunction());

        // when
        AmwFunctionEntity overwriteFunction = functionService.overwriteResourceFunction("functionBody", rootFunction, resource);

        // then
        assertTrue(rootFunction.isOverwrittenBySubTypeOrResourceFunction());
        assertEquals(rootFunction, overwriteFunction.getOverwrittenParent());
        assertTrue(rootFunction.getOverwritingChildFunction().contains(overwriteFunction));
    }

    @Test
    public void overwriteResourceFunctionShouldDelegatePersistOrMerge() {
        // given
        AmwFunctionEntity rootFunction = createFunctionForResourceType(FUNCTION_NAME_A, FUNCTION_ID_1, rootResourceType, "mik1Name");
        ResourceEntity resource = createResourceWithType("resource", 99, rootResourceType);
        assertFalse(rootFunction.isOverwrittenBySubTypeOrResourceFunction());

        // when
        AmwFunctionEntity overwriteFunction = functionService.overwriteResourceFunction("functionBody", rootFunction, resource);

        // then
        verify(functionRepositoryMock).persistOrMergeFunction(overwriteFunction);
    }


    @Test
    public void overwriteResourceFunctionWhenOverwrittenInOtherNamespaceResourceShouldOverwriteFunctionInFunctionNamespace() {
        // given
        AmwFunctionEntity rootFunction = createFunctionForResourceType(FUNCTION_NAME_A, FUNCTION_ID_1, rootResourceType, "mik1Name");
        ResourceTypeEntity resourceType = createSubResourceType(rootResourceType);
        ResourceEntity resource = createResourceWithType("resource", 99, resourceType);

        ResourceTypeEntity otherSubResourceType = createSubResourceType(rootResourceType);

        createSubResourceType(otherSubResourceType);

        AmwFunctionEntity otherOverwritingFunction = createOverwritingFunction(rootFunction, 99);
        createResourceWithType("otherResource", 111, otherSubResourceType, otherOverwritingFunction);
        assertTrue(rootFunction.isOverwrittenBySubTypeOrResourceFunction());

        // when
        AmwFunctionEntity overwriteFunction = functionService.overwriteResourceFunction("functionBody", rootFunction, resource);

        // then
        assertEquals(rootFunction, overwriteFunction.getOverwrittenParent());
        assertTrue(rootFunction.getOverwritingChildFunction().contains(overwriteFunction));
        assertTrue(rootFunction.getOverwritingChildFunction().contains(otherOverwritingFunction));
    }

    @Test
    public void overwriteResourceTypeFunctionShouldCopyRootName() {
        // given
        String mik1Name = "mik1";
        AmwFunctionEntity rootFunction = createFunctionForResourceType(FUNCTION_NAME_A, FUNCTION_ID_1, rootResourceType, mik1Name);
        ResourceTypeEntity resourceType = createSubResourceType(rootResourceType);
        assertFalse(rootFunction.isOverwrittenBySubTypeOrResourceFunction());

        // when
        AmwFunctionEntity overwriteFunction = functionService.overwriteResourceTypeFunction("functionBody", rootFunction, resourceType);

        // then
        assertEquals(rootFunction.getName(), overwriteFunction.getName());
    }

    @Test
    public void overwriteResourceTypeFunctionShouldSetFunctionBody() {
        // given
        String functionBody = "functionBody";
        String mik1Name = "mik1";
        AmwFunctionEntity rootFunction = createFunctionForResourceType(FUNCTION_NAME_A, FUNCTION_ID_1, rootResourceType, mik1Name);
        ResourceTypeEntity resourceType = createSubResourceType(rootResourceType);
        assertFalse(rootFunction.isOverwrittenBySubTypeOrResourceFunction());

        // when
        AmwFunctionEntity overwriteFunction = functionService.overwriteResourceTypeFunction(functionBody, rootFunction, resourceType);

        // then
        assertEquals(functionBody, overwriteFunction.getImplementation());
    }

    @Test
    public void overwriteResourceTypeFunctionShouldCopyRootMiks() {
        // given
        String mik1Name = "mik1";
        AmwFunctionEntity rootFunction = createFunctionForResourceType(FUNCTION_NAME_A, FUNCTION_ID_1, rootResourceType, mik1Name);
        ResourceTypeEntity resourceType = createSubResourceType(rootResourceType);
        assertFalse(rootFunction.isOverwrittenBySubTypeOrResourceFunction());

        // when
        AmwFunctionEntity overwriteFunction = functionService.overwriteResourceTypeFunction("functionBody", rootFunction, resourceType);

        // then
        assertEquals(rootFunction.getMikNames().size(), overwriteFunction.getMikNames().size());
        assertEquals(rootFunction.getCommaseparatedMikNames(), overwriteFunction.getCommaseparatedMikNames());
    }

    @Test
    public void overwriteResourceTypeFunctionShouldNotSetId() {
        // given
        String mik1Name = "mik1";
        AmwFunctionEntity rootFunction = createFunctionForResourceType(FUNCTION_NAME_A, FUNCTION_ID_1, rootResourceType, mik1Name);
        ResourceTypeEntity resourceType = createSubResourceType(rootResourceType);
        assertFalse(rootFunction.isOverwrittenBySubTypeOrResourceFunction());

        // when
        AmwFunctionEntity overwriteFunction = functionService.overwriteResourceTypeFunction("functionBody", rootFunction, resourceType);

        // then
        assertNotEquals(FUNCTION_ID_1, overwriteFunction.getId());
    }

    @Test
    public void overwriteResourceTypeFunctionShouldSetResourceTypeToOverwritingFunction() {
        // given
        String mik1Name = "mik1";
        AmwFunctionEntity rootFunction = createFunctionForResourceType(FUNCTION_NAME_A, FUNCTION_ID_1, rootResourceType, mik1Name);
        ResourceTypeEntity resourceType = createSubResourceType(rootResourceType);
        assertFalse(rootFunction.isOverwrittenBySubTypeOrResourceFunction());

        // when
        AmwFunctionEntity overwriteFunction = functionService.overwriteResourceTypeFunction("functionBody", rootFunction, resourceType);

        // then
        assertEquals(resourceType, overwriteFunction.getResourceType());
    }


    @Test
    public void overwriteResourceTypeFunctionShouldSetParentAndChildFunction() {
        // given
        AmwFunctionEntity rootFunction = createFunctionForResourceType(FUNCTION_NAME_A, FUNCTION_ID_1, rootResourceType, "mik1Name");
        ResourceTypeEntity resourceType = createSubResourceType(rootResourceType);
        assertFalse(rootFunction.isOverwrittenBySubTypeOrResourceFunction());

        // when
        AmwFunctionEntity overwriteFunction = functionService.overwriteResourceTypeFunction("functionBody", rootFunction, resourceType);

        // then
        assertTrue(rootFunction.isOverwrittenBySubTypeOrResourceFunction());
        assertEquals(rootFunction, overwriteFunction.getOverwrittenParent());
        assertTrue(rootFunction.getOverwritingChildFunction().contains(overwriteFunction));
    }

    @Test
    public void overwriteResourceTypeFunctionShouldDelegatePersistOrMerge() {
        // given
        AmwFunctionEntity rootFunction = createFunctionForResourceType(FUNCTION_NAME_A, FUNCTION_ID_1, rootResourceType, "mik1Name");
        ResourceTypeEntity resourceType = createSubResourceType(rootResourceType);
        assertFalse(rootFunction.isOverwrittenBySubTypeOrResourceFunction());

        // when
        AmwFunctionEntity overwriteFunction = functionService.overwriteResourceTypeFunction("functionBody", rootFunction, resourceType);

        // then
        verify(functionRepositoryMock).persistOrMergeFunction(overwriteFunction);
    }

    @Test
    public void overwriteResourceTypeFunctionWhenOverwrittenInOtherNamespaceSubResourceTypeShouldOverwriteFunctionInFunctionNamespace() {
        // given
        AmwFunctionEntity rootFunction = createFunctionForResourceType(FUNCTION_NAME_A, FUNCTION_ID_1, rootResourceType, "mik1Name");
        ResourceTypeEntity resourceType = createSubResourceType(rootResourceType);

        AmwFunctionEntity otherOverwritingFunction = createOverwritingFunction(rootFunction, 99);
        createSubResourceType(rootResourceType, otherOverwritingFunction);
        assertTrue(rootFunction.isOverwrittenBySubTypeOrResourceFunction());

        // when
        AmwFunctionEntity overwriteFunction = functionService.overwriteResourceTypeFunction("functionBody", rootFunction, resourceType);

        // then
        assertEquals(rootFunction, overwriteFunction.getOverwrittenParent());
        assertTrue(rootFunction.getOverwritingChildFunction().contains(overwriteFunction));
        assertTrue(rootFunction.getOverwritingChildFunction().contains(otherOverwritingFunction));
    }

    @Test
    public void overwriteResourceTypeFunctionWhenOverwrittenInOtherNamespaceSubSubResourceTypeShouldOverwriteFunctionInFunctionNamespace() {
        // given
        AmwFunctionEntity rootFunction = createFunctionForResourceType(FUNCTION_NAME_A, FUNCTION_ID_1, rootResourceType, "mik1Name");
        ResourceTypeEntity resourceType = createSubResourceType(rootResourceType);

        ResourceTypeEntity otherSubResourceType = createSubResourceType(rootResourceType);

        AmwFunctionEntity otherOverwritingFunction = createOverwritingFunction(rootFunction, 99);
        createSubResourceType(otherSubResourceType, otherOverwritingFunction);
        assertTrue(rootFunction.isOverwrittenBySubTypeOrResourceFunction());

        // when
        AmwFunctionEntity overwriteFunction = functionService.overwriteResourceTypeFunction("functionBody", rootFunction, resourceType);

        // then
        assertEquals(rootFunction, overwriteFunction.getOverwrittenParent());
        assertTrue(rootFunction.getOverwritingChildFunction().contains(overwriteFunction));
        assertTrue(rootFunction.getOverwritingChildFunction().contains(otherOverwritingFunction));
    }

    @Test
    public void overwriteResourceTypeFunctionWhenOverwrittenInOtherNamespaceResourceShouldOverwriteFunctionInFunctionNamespace() {
        // given
        AmwFunctionEntity rootFunction = createFunctionForResourceType(FUNCTION_NAME_A, FUNCTION_ID_1, rootResourceType, "mik1Name");
        ResourceTypeEntity resourceType = createSubResourceType(rootResourceType);

        ResourceTypeEntity otherSubResourceType = createSubResourceType(rootResourceType);

        createSubResourceType(otherSubResourceType);

        AmwFunctionEntity otherOverwritingFunction = createOverwritingFunction(rootFunction, 99);
        createResourceWithType("otherResource", 111, otherSubResourceType, otherOverwritingFunction);
        assertTrue(rootFunction.isOverwrittenBySubTypeOrResourceFunction());

        // when
        AmwFunctionEntity overwriteFunction = functionService.overwriteResourceTypeFunction("functionBody", rootFunction, resourceType);

        // then
        assertEquals(rootFunction, overwriteFunction.getOverwrittenParent());
        assertTrue(rootFunction.getOverwritingChildFunction().contains(overwriteFunction));
        assertTrue(rootFunction.getOverwritingChildFunction().contains(otherOverwritingFunction));
    }

    @Test(expected = RuntimeException.class)
    public void overwriteResourceTypeFunctionOnRootResourceTypeShouldThrowException() {
        // given
        AmwFunctionEntity rootResourceTypeFunction = createFunctionForResourceType(FUNCTION_NAME_A, FUNCTION_ID_1, rootResourceType, "mik1Name");

        // when
        functionService.overwriteResourceTypeFunction("functionBody", rootResourceTypeFunction, rootResourceType);

    }

    @Test
    public void overwriteResourceTypeFunctionOnSubResourceTypeWhenOverwrittenInResourceOfSubSubResourceTypeShouldReplaceOverwriteFunction() {
        // given
        AmwFunctionEntity rootResourceTypeFunction = createFunctionForResourceType(FUNCTION_NAME_A, FUNCTION_ID_1, rootResourceType, "mik1Name");

        ResourceTypeEntity subResourceType = createSubResourceType(rootResourceType);
        ResourceTypeEntity subSubResourceType = createSubResourceType(subResourceType);

        AmwFunctionEntity overwritingResourceFunction = createOverwritingFunction(rootResourceTypeFunction, 99);
        createResourceWithType("otherResource", 111, subSubResourceType, overwritingResourceFunction);

        assertEquals(rootResourceTypeFunction, overwritingResourceFunction.getOverwrittenParent());
        assertTrue(rootResourceTypeFunction.getOverwritingChildFunction().contains(overwritingResourceFunction));
        assertTrue(overwritingResourceFunction.getOverwritingChildFunction().isEmpty());

        // when
        AmwFunctionEntity overwritingSubResourceTypeFunction = functionService.overwriteResourceTypeFunction("functionBody", rootResourceTypeFunction, subResourceType);

        // then
        assertEquals(rootResourceTypeFunction, overwritingSubResourceTypeFunction.getOverwrittenParent());
        assertTrue(rootResourceTypeFunction.getOverwritingChildFunction().contains(overwritingSubResourceTypeFunction));
        assertTrue(overwritingSubResourceTypeFunction.getOverwritingChildFunction().contains(overwritingResourceFunction));

        assertEquals(overwritingSubResourceTypeFunction, overwritingResourceFunction.getOverwrittenParent());
        assertTrue(overwritingResourceFunction.getOverwritingChildFunction().isEmpty());
    }

    @Test
    public void overwriteResourceTypeFunctionOnSubSubResourceTypeWhenOverwrittenInResourceOfSubSubResourceTypeShouldReplaceOverwriteFunction() {
        // given
        AmwFunctionEntity rootResourceTypeFunction = createFunctionForResourceType(FUNCTION_NAME_A, FUNCTION_ID_1, rootResourceType, "mik1Name");

        ResourceTypeEntity subResourceType = createSubResourceType(rootResourceType);
        ResourceTypeEntity subSubResourceType = createSubResourceType(subResourceType);

        AmwFunctionEntity overwritingResourceFunction = createOverwritingFunction(rootResourceTypeFunction, 99);
        createResourceWithType("otherResource", 111, subSubResourceType, overwritingResourceFunction);

        assertEquals(rootResourceTypeFunction, overwritingResourceFunction.getOverwrittenParent());
        assertTrue(rootResourceTypeFunction.getOverwritingChildFunction().contains(overwritingResourceFunction));
        assertTrue(overwritingResourceFunction.getOverwritingChildFunction().isEmpty());

        // when
        AmwFunctionEntity overwritingSubSubResourceTypeFunction = functionService.overwriteResourceTypeFunction("functionBody", rootResourceTypeFunction, subSubResourceType);

        // then
        assertEquals(rootResourceTypeFunction, overwritingSubSubResourceTypeFunction.getOverwrittenParent());
        assertTrue(rootResourceTypeFunction.getOverwritingChildFunction().contains(overwritingSubSubResourceTypeFunction));
        assertTrue(overwritingSubSubResourceTypeFunction.getOverwritingChildFunction().contains(overwritingResourceFunction));

        assertEquals(overwritingSubSubResourceTypeFunction, overwritingResourceFunction.getOverwrittenParent());
        assertTrue(overwritingResourceFunction.getOverwritingChildFunction().isEmpty());
    }

    @Test
    public void overwriteResourceTypeFunctionOnSubResourceTypeWhenOverwrittenInSubSubResourceTypeShouldReplaceOverwriteFunction() {
        // given
        AmwFunctionEntity rootResourceTypeFunction = createFunctionForResourceType(FUNCTION_NAME_A, FUNCTION_ID_1, rootResourceType, "mik1Name");

        ResourceTypeEntity subResourceType = createSubResourceType(rootResourceType);

        AmwFunctionEntity overwritingSubSubResourceTypFunction = createOverwritingFunction(rootResourceTypeFunction, 99);
        createSubResourceType(subResourceType, overwritingSubSubResourceTypFunction);

        assertEquals(rootResourceTypeFunction, overwritingSubSubResourceTypFunction.getOverwrittenParent());
        assertTrue(rootResourceTypeFunction.getOverwritingChildFunction().contains(overwritingSubSubResourceTypFunction));
        assertTrue(overwritingSubSubResourceTypFunction.getOverwritingChildFunction().isEmpty());

        // when
        AmwFunctionEntity overwritingSubSubResourceTypeFunction = functionService.overwriteResourceTypeFunction("functionBody", rootResourceTypeFunction, subResourceType);

        // then
        assertEquals(rootResourceTypeFunction, overwritingSubSubResourceTypeFunction.getOverwrittenParent());
        assertTrue(rootResourceTypeFunction.getOverwritingChildFunction().contains(overwritingSubSubResourceTypeFunction));
        assertTrue(overwritingSubSubResourceTypeFunction.getOverwritingChildFunction().contains(overwritingSubSubResourceTypFunction));

        assertEquals(overwritingSubSubResourceTypeFunction, overwritingSubSubResourceTypFunction.getOverwrittenParent());
        assertTrue(overwritingSubSubResourceTypFunction.getOverwritingChildFunction().isEmpty());
    }

    @Test(expected = RuntimeException.class)
    public void overwriteResourceTypeFunctionOnSubSubResourceTypeWhenOverwrittenInSubSubResourceTypeShouldReplaceOverwriteFunction() {
        // given
        AmwFunctionEntity rootResourceTypeFunction = createFunctionForResourceType(FUNCTION_NAME_A, FUNCTION_ID_1, rootResourceType, "mik1Name");

        ResourceTypeEntity subResourceType = createSubResourceType(rootResourceType);

        AmwFunctionEntity overwritingSubSubResourceTypFunction = createOverwritingFunction(rootResourceTypeFunction, 99);
        ResourceTypeEntity subSubResourceType = createSubResourceType(subResourceType, overwritingSubSubResourceTypFunction);

        assertEquals(rootResourceTypeFunction, overwritingSubSubResourceTypFunction.getOverwrittenParent());
        assertTrue(rootResourceTypeFunction.getOverwritingChildFunction().contains(overwritingSubSubResourceTypFunction));
        assertTrue(overwritingSubSubResourceTypFunction.getOverwritingChildFunction().isEmpty());

        // when
        functionService.overwriteResourceTypeFunction("functionBody", rootResourceTypeFunction, subSubResourceType);

    }

    @Test
    public void deleteFunctionShouldDelegateRemoveFunction() {
        // given
        AmwFunctionEntity rootFunction = createFunction(FUNCTION_NAME_A, FUNCTION_ID_1);

        // when
        functionService.deleteFunction(rootFunction);

        // then
        verify(functionRepositoryMock).remove(rootFunction);
    }


    @Test
    public void deleteFunctionWhenOverwritingFunctionShouldResetOverwriting() {
        // given
        AmwFunctionEntity rootFunction = createFunction(FUNCTION_NAME_A, FUNCTION_ID_1);
        AmwFunctionEntity subFunction = createFunction(FUNCTION_NAME_A, FUNCTION_ID_2);
        subFunction.overwrite(rootFunction);
        assertTrue(rootFunction.isOverwrittenBySubTypeOrResourceFunction());

        // when
        functionService.deleteFunction(subFunction);

        // then
        assertFalse(rootFunction.isOverwrittenBySubTypeOrResourceFunction());
    }

	private AmwFunctionEntity createFunction(String name, Integer id, String... mikNames) {
        Set<MikEntity> mikEntities = new HashSet<>();
        AmwFunctionEntity amwFunctionEntity = new AmwFunctionEntityBuilder(name, id).build();

        for (String mikName : mikNames){
            mikEntities.add(new MikEntity(mikName, amwFunctionEntity));
        }
        amwFunctionEntity.setMiks(mikEntities);
		return amwFunctionEntity;
	}

    @Test
    public void verifyCreateFunctionTestMethod(){
        // given
        String name = "name";
        Integer id = 1;

        // when
        AmwFunctionEntity fct = createFunction(name, id);

        // then
        assertEquals(name, fct.getName());
        assertEquals(id, fct.getId());
    }

    @Test
    public void verifyCreateFunctionWithMiksTestMethod(){
        // given
        String name = "name";
        Integer id = 1;
        String mikName1 = "MikName1";
        String mikName2 = "MikName2";

        // when
        AmwFunctionEntity fct = createFunction(name, id, mikName1, mikName2);

        // then
        assertTrue(fct.getMikNames().contains(mikName1));
        assertTrue(fct.getMikNames().contains(mikName2));
    }

    private ResourceTypeEntity createRootResourceType(AmwFunctionEntity... functions) {
        ResourceTypeEntity resourceType = new ResourceTypeEntityBuilder().buildResourceTypeEntity("RootResourceType", new HashSet<ResourceEntity>(), true);
        resourceType.setParentResourceType(null);

        resourceType.setFunctions(new HashSet<>(Arrays.asList(functions)));

        for (AmwFunctionEntity function : functions) {
            function.setResourceType(resourceType);
        }

        return resourceType;
    }

    @Test
    public void verifyCreateRootResourceTypeWithoutFunctionsTestMethod(){
        // given

        // when
        ResourceTypeEntity rootResourceType = createRootResourceType();

        // then
        assertTrue(rootResourceType.getFunctions().isEmpty());
        assertTrue(rootResourceType.isRootResourceType());
        assertEquals("RootResourceType", rootResourceType.getName());
    }

    @Test
    public void verifyCreateRootResourceTypeWithFunctionsTestMethod(){
        // given
        AmwFunctionEntity function = FUNCTION_A;

        // when
        ResourceTypeEntity rootResourceType = createRootResourceType(function);

        // then
        assertTrue(rootResourceType.isRootResourceType());
        assertTrue(rootResourceType.getFunctions().contains(FUNCTION_A));
        assertEquals(FUNCTION_A.getResourceType(), rootResourceType);
    }

    private ResourceTypeEntity createSubResourceType(ResourceTypeEntity parentResourceType, AmwFunctionEntity... functions) {
        ResourceTypeEntity subResourceType = new ResourceTypeEntityBuilder().buildResourceTypeEntity("Sub"+ (parentResourceType != null ? parentResourceType.getName():"resourceType"), new HashSet<ResourceEntity>(), true);

        if (parentResourceType != null) {
            Set<ResourceTypeEntity> chiltResourceTypes = new HashSet<>();
            chiltResourceTypes.add(subResourceType);
            parentResourceType.setChildrenResourceTypes(chiltResourceTypes);
        }

        subResourceType.setParentResourceType(parentResourceType);

        subResourceType.setFunctions(new HashSet<>(Arrays.asList(functions)));

        for (AmwFunctionEntity function : functions) {
            function.setResourceType(subResourceType);
        }

        return subResourceType;
    }

    @Test
    public void verifyCreateSubResourceTypeWithoutFunctionsTestMethod(){
        // given
        ResourceTypeEntity parentResourceType = createRootResourceType();

        // when
        ResourceTypeEntity subResourceType = createSubResourceType(parentResourceType);

        // then
        assertTrue(subResourceType.getFunctions().isEmpty());
        assertFalse(subResourceType.isRootResourceType());
        assertEquals(parentResourceType, subResourceType.getParentResourceType());
        assertTrue(parentResourceType.getChildrenResourceTypes().contains(subResourceType));
    }

    @Test
    public void verifyCreateSubResourceTypeWithFunctionsTestMethod(){
        // given
        ResourceTypeEntity parentResourceType = createRootResourceType();

        // when
        ResourceTypeEntity subResourceType = createSubResourceType(parentResourceType, FUNCTION_A);

        // then
        assertFalse(subResourceType.getFunctions().isEmpty());
        assertTrue(subResourceType.getFunctions().contains(FUNCTION_A));
        assertEquals(subResourceType, FUNCTION_A.getResourceType());
    }

    private AmwFunctionEntity createFunctionForResourceType(String name, Integer id, ResourceTypeEntity resourceType, String... mikNames) {
        AmwFunctionEntity amwFunctionEntity = createFunction(name, id, mikNames);

        amwFunctionEntity.setResourceType(resourceType);
        resourceType.addFunction(amwFunctionEntity);

        return amwFunctionEntity;
    }

    @Test
    public void verifyCreateFunctionForResourceTypeTestMethod(){
        // given
        String name = "functionName";
        Integer id = 1;
        ResourceTypeEntity rootResourceType = createRootResourceType();

        // when
        AmwFunctionEntity functionForResourceType = createFunctionForResourceType(name, id, rootResourceType);

        // then
        assertEquals(name, functionForResourceType.getName());
        assertEquals(id, functionForResourceType.getId());
        assertEquals(rootResourceType, functionForResourceType.getResourceType());
        assertTrue(rootResourceType.getFunctions().contains(functionForResourceType));
    }

    private AmwFunctionEntity createOverwritingFunction(AmwFunctionEntity functionToOverwrite, Integer overwritingId){
        AmwFunctionEntity overwritingFunction = createFunction(functionToOverwrite.getName(), overwritingId);
        overwritingFunction.overwrite(functionToOverwrite);
        return overwritingFunction;
    }

    @Test
    public void verifyCreateOverwritingFunctionTestMethod(){
        // given
        AmwFunctionEntity functionToOverwrite = FUNCTION_A;
        Integer id = 1;

        // when
        AmwFunctionEntity overwritingFunction = createOverwritingFunction(functionToOverwrite, id);

        // then
        assertEquals(functionToOverwrite.getName(), overwritingFunction.getName());
        assertEquals(id, overwritingFunction.getId());
        assertTrue(functionToOverwrite.isOverwrittenBySubTypeOrResourceFunction());
        assertTrue(functionToOverwrite.getOverwritingChildFunction().contains(overwritingFunction));
        assertEquals(overwritingFunction.getOverwrittenParent(), functionToOverwrite);
    }

	private ResourceEntity createResource(String name, int id, AmwFunctionEntity... functions) {
		ResourceEntity resource = new ResourceEntityBuilder().withName(name).withId(id).withFunctions(new HashSet<>(Arrays.asList(functions))).build();
		for (AmwFunctionEntity function : functions) {
			function.setResource(resource);
		}
		return resource;
	}

    @Test
    public void verifyCreateResourceWithoutFunctionTestMethod(){
        // given
        String name = "name";
        Integer id = 1;

        // when
        ResourceEntity resource = createResource(name, id);

        // then
        assertEquals(name, resource.getName());
        assertEquals(id, resource.getId());
        assertTrue(resource.getFunctions().isEmpty());
    }

    @Test
    public void verifyCreateResourceWithFunctionTestMethod(){
        // given
        String name = "name";
        Integer id = 1;
        AmwFunctionEntity function1 = createFunction("function1", 1);
        AmwFunctionEntity function2 = createFunction("function2", 2);

        // when
        ResourceEntity resource = createResource(name, id, function1, function2);

        // then
        assertTrue(resource.getFunctions().contains(function1));
        assertTrue(resource.getFunctions().contains(function2));
        for(AmwFunctionEntity function : resource.getFunctions()){
            assertEquals(resource, function.getResource());
        }
    }

	private ResourceEntity createResourceWithType(String name, int id, ResourceTypeEntity resourceType, AmwFunctionEntity... functions) {
		ResourceEntity resource = createResource(name, id, functions);
		resource.setResourceType(resourceType);
		Set<ResourceEntity> resources = resourceType.getResources();
		resources.add(resource);
		resourceType.setResources(resources);
		return resource;
	}

    @Test
    public void verifyCreateResourceWithTypeWithoutFunctionTestMethod(){
        // given
        String name = "name";
        Integer id = 1;
        ResourceTypeEntity resourceType = createRootResourceType();

        // when
        ResourceEntity resource = createResourceWithType(name, id, resourceType);

        // then
        assertEquals(resourceType, resource.getResourceType());
        assertTrue(resourceType.getResources().contains(resource));
    }


}