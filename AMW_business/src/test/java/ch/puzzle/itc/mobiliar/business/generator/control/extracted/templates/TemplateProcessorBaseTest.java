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

package ch.puzzle.itc.mobiliar.business.generator.control.extracted.templates;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.FileExistsException;
import org.apache.commons.io.FileUtils;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;

import com.google.common.collect.Lists;

import ch.puzzle.itc.mobiliar.business.deploy.entity.DeploymentEntity;
import ch.puzzle.itc.mobiliar.business.environment.entity.ContextEntity;
import ch.puzzle.itc.mobiliar.business.foreignable.entity.ForeignableOwner;
import ch.puzzle.itc.mobiliar.business.function.control.FunctionService;
import ch.puzzle.itc.mobiliar.business.generator.control.AMWTemplateExceptionHandler;
import ch.puzzle.itc.mobiliar.business.generator.control.GeneratedTemplate;
import ch.puzzle.itc.mobiliar.business.generator.control.GenerationUnitGenerationResult;
import ch.puzzle.itc.mobiliar.business.generator.control.GeneratorFileWriter;
import ch.puzzle.itc.mobiliar.business.generator.control.GeneratorUtils;
import ch.puzzle.itc.mobiliar.business.generator.control.extracted.GenerationContext;
import ch.puzzle.itc.mobiliar.business.generator.control.extracted.GenerationModus;
import ch.puzzle.itc.mobiliar.business.generator.control.extracted.ResourceDependencyResolverService;
import ch.puzzle.itc.mobiliar.business.releasing.entity.ReleaseEntity;
import ch.puzzle.itc.mobiliar.business.resourcegroup.entity.ResourceEntity;
import ch.puzzle.itc.mobiliar.test.CustomLogging;
import ch.puzzle.itc.mobiliar.test.EntityBuilder;
import ch.puzzle.itc.mobiliar.test.EntityBuilderType;

public class TemplateProcessorBaseTest<T extends EntityBuilder> {

	public T builder;

	public ContextEntity context;
	public List<File> files;

	List<GenerationUnitGenerationResult> generationResults;

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();

	@Spy
	Logger log = Logger.getLogger(AppServerRelationsTemplateProcessor.class.getSimpleName());

	@Spy
	GeneratorUtils utils;

	@Spy
	ResourceDependencyResolverService dependencyResolverService;

	@InjectMocks
	GeneratorFileWriter writer;

	@Spy
	protected AMWTemplateExceptionHandler templateExceptionHandler;

	@InjectMocks
	protected AppServerRelationsTemplateProcessor processor;

	@InjectMocks
	protected GenerationUnitFactory factory;
	

	@Mock
	FunctionService FunctionService;

	protected  GenerationPackage work;
	protected GenerationOptions options;


	protected void generate(AMWTemplateExceptionHandler templateExceptionHandler) throws IOException {
		CustomLogging.setup(Level.OFF);
		prepareWorkUnits(templateExceptionHandler);
		generateTemplates();
		writeFiles();
	}

	protected void prepareWorkUnits(AMWTemplateExceptionHandler templateExceptionHandler) {
		if (context == null) {
			throw new RuntimeException("context is null");
		}
		options = createOptions();
	    	factory.dependencyResolver = dependencyResolverService;
		work = factory.createWorkForAppServer(options, builder.as, templateExceptionHandler);
	}

	protected GenerationOptions createOptions() {
		
		DeploymentEntity d = new DeploymentEntity();
		d.setRuntime(builder.platform);
		d.setApplicationsWithVersion(new ArrayList<DeploymentEntity.ApplicationWithVersion>());
		
		ReleaseEntity release = new ReleaseEntity();
		Calendar cal = new GregorianCalendar();
		release.setId(-1);
		release.setName("release");
		cal.set(2014, Calendar.JANUARY, 1);
		d.setRelease(release );

	    GenerationContext context = new GenerationContext(this.context, builder.as, d, null,
			    GenerationModus.SIMULATE,
			    dependencyResolverService);
	    context.setNode(builder.buildResource(builder.buildResourceType(EntityBuilderType.NODE1.type), "node"));
	    builder.buildConsumedRelation(context.getApplicationServer(), context.getNode(), ForeignableOwner.AMW);
	    return new GenerationOptions(context);
	}


	protected void generateTemplates() throws IOException {
		generationResults = generateTemplates(options, work, processor);
	}
	
	protected int getGeneratedTemplateSize(){
		ArrayList<GeneratedTemplate> templates = new ArrayList<GeneratedTemplate>();
		for (GenerationUnitGenerationResult result : generationResults) {
			templates.addAll(result.getGeneratedTemplates());
		}
		return templates.size();
	}

	protected void writeFiles() throws FileExistsException, IOException {
		writer.generateFileStructure(generationResults, folder.getRoot().toString(), null);
		files = Lists.newArrayList(new File(folder.getRoot().toString()).listFiles());
	}

	protected String readFile(String name) throws IOException {
		for (File file : files) {
			if (file.getName().equals(name)) {
				return FileUtils.readFileToString(file, StandardCharsets.UTF_8);
			}
		}
		throw new RuntimeException("file not found: " + name);
	}
	
	public static List<GenerationUnitGenerationResult> generateTemplates(GenerationOptions options, GenerationPackage work, AppServerRelationsTemplateProcessor processor) throws IOException{
		List<GenerationUnitGenerationResult> templates = Lists.newArrayList();
		work.setGenerationOptions(options);
		processor.setGlobals(work);
		processor.setGenerationContext(options.getContext());

		for (Entry<ResourceEntity, Set<GenerationUnit>> entry : work.getAppGenerationBatches().entrySet()) {
			List<GenerationUnitGenerationResult> results = processor.generateApp(entry.getValue(), entry.getKey());
			templates.addAll(results);
		}

		GenerationUnit.forResource(work.getNodeGenerationUnits(), options.getContext().getNode());

		for (GenerationUnit unit : work.getNodeGenerationUnits()) {
			if(!unit.isTemplateGenerationDisabled()) {
				ResourceEntity node = unit.getSlaveResource();

				List<GenerationUnitGenerationResult> results = processor.generateAppServer(GenerationUnit.forAppServer(work.getAsSet(), node), node);
				templates.addAll(results);
			}
		}

		return templates;
	}

}
