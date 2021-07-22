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

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import ch.puzzle.itc.mobiliar.business.function.entity.AmwFunctionEntity;
import ch.puzzle.itc.mobiliar.business.generator.control.*;
import ch.puzzle.itc.mobiliar.business.globalfunction.entity.GlobalFunctionEntity;
import ch.puzzle.itc.mobiliar.business.property.entity.AmwTemplateModel;
import ch.puzzle.itc.mobiliar.business.template.entity.TemplateDescriptorEntity;
import ch.puzzle.itc.mobiliar.common.exception.TemplatePropertyException;
import ch.puzzle.itc.mobiliar.common.exception.TemplatePropertyException.CAUSE;

import freemarker.cache.StringTemplateLoader;
import freemarker.core.ParseException;
import freemarker.template.Configuration;
import freemarker.template.TemplateException;

/**
 * Handles low level rendering of freemarker templates.
 */
public class BaseTemplateProcessor {

    protected Logger log = Logger.getLogger(BaseTemplateProcessor.class.getName());

    protected static String FILEPATH_PLACEHOLDER = "_FILEPATH";

    /**
     * Generates the templates for a specific resource
     *
     * @throws IOException
     */
    protected GenerationUnitGenerationResult generateResourceTemplates(GenerationUnit unit, AmwTemplateModel model) throws IOException {
        GenerationUnitPreprocessResult generationUnitPreprocessResult = preProcessModel(model);
        GenerationUnitGenerationResult generationUnitGenerationResult = generateTemplatesAmwTemplateModel(unit.getTemplates(), unit.getGlobalFunctionTemplates(), model);

        generationUnitGenerationResult.setGenerationUnitPreprocessResult(generationUnitPreprocessResult);

        return generationUnitGenerationResult;
    }

    /**
     * Generates the RelationTemplates based on the given AmwTemplateModel
     *
     * @throws IOException
     */
    protected GenerationUnitGenerationResult generateResourceRelationTemplates(GenerationUnit unit, AmwTemplateModel model)
            throws IOException {
        GenerationUnitPreprocessResult generationUnitPreprocessResult = preProcessModel(model);
        GenerationUnitGenerationResult generationUnitGenerationResult = generateTemplatesAmwTemplateModel(unit.getRelationTemplates(), unit.getGlobalFunctionTemplates(), model);

        generationUnitGenerationResult.setGenerationUnitPreprocessResult(generationUnitPreprocessResult);

        return generationUnitGenerationResult;
    }

    private GenerationUnitPreprocessResult preProcessModel(AmwTemplateModel model) {
        AmwModelPreprocessExceptionHandler exceptionHandler = new AmwModelPreprocessExceptionHandler();
        model.setAmwModelPreprocessExceptionHandler(exceptionHandler);
        model.preProcess();

        GenerationUnitPreprocessResult result = new GenerationUnitPreprocessResult();
        result.setErrorMessages(exceptionHandler.getErrorMessages());
        return result;
    }

    private GenerationUnitGenerationResult generateTemplatesAmwTemplateModel(Set<TemplateDescriptorEntity> templates, List<GlobalFunctionEntity> globalFunctions,
                                                                             AmwTemplateModel model) throws IOException {

        //logPropertiesForTemplates(templates, contextualizedMap);
        AMWTemplateExceptionHandler templateExceptionHandler = new AMWTemplateExceptionHandler();

        Configuration cfg = populateConfig(templates, globalFunctions, templateExceptionHandler);
        GenerationUnitGenerationResult result = new GenerationUnitGenerationResult();
        List<GeneratedTemplate> generatedTemplates = new ArrayList<>();
        for (TemplateDescriptorEntity template : templates) {
            try {
                GeneratedTemplate generatedTemplate = generateAmwTemplateModel(cfg, template, model);
                generatedTemplate.addAllErrorMessages(templateExceptionHandler.getErrorMessages());
                generatedTemplates.add(generatedTemplate);
                // reset handler
                templateExceptionHandler.reset();
            } catch (TemplateException te) {
                GeneratedTemplate errorTemplate = new GeneratedTemplate(template.getName(), template.getTargetPath(), "");
                //logBeforeException(te, contextualizedMap);
                errorTemplate.addAllErrorMessages(Collections.singletonList(new TemplatePropertyException(
                        "missing property value or propertydefinition in template. " + te.getMessage(),
                        CAUSE.INVALID_PROPERTY, te)));
                generatedTemplates.add(errorTemplate);
            } catch (ParseException pe) {
                // Validation failed! - was not able to parse the template!
                GeneratedTemplate errorTemplate = new GeneratedTemplate(template.getName(), template.getTargetPath(), "");
                logBeforeException(pe);
                errorTemplate.addAllErrorMessages(Collections.singletonList(new TemplatePropertyException(
                        "invalid template. " + pe.getMessage(),
                        CAUSE.PROCESSING_EXCEPTION, pe)));
                generatedTemplates.add(errorTemplate);
            } catch (IOException ioe) {
                log.log(Level.WARNING, ioe.getMessage());
                throw ioe;
            }
        }

        result.setGeneratedTemplates(generatedTemplates);
        return result;
    }

    private Configuration populateConfig(Set<TemplateDescriptorEntity> templates, List<GlobalFunctionEntity> globalFunctionTemplates,
                                         AMWTemplateExceptionHandler templateExceptionHandler) {
        Configuration cfg = getConfiguration(templateExceptionHandler);
        StringTemplateLoader loader = new StringTemplateLoader();

        for (TemplateDescriptorEntity template : templates) {
            loader.putTemplate(template.getName(), template.getFileContent());

            if (template.getTargetPath() != null && !template.getTargetPath().isEmpty()) {
                loader.putTemplate(template.getName() + BaseTemplateProcessor.FILEPATH_PLACEHOLDER,
                        template.getTargetPath());
            }
        }
        addGlobalFunctionTemplates(globalFunctionTemplates, loader);
        cfg.setTemplateLoader(loader);
        return cfg;
    }

    private void addGlobalFunctionTemplates(List<GlobalFunctionEntity> globalFunctionTemplates,
                                            StringTemplateLoader loader) {
        if (globalFunctionTemplates != null) {
            for (GlobalFunctionEntity template : globalFunctionTemplates) {
                loader.putTemplate(template.getName(), template.getContent());
            }
        }
    }

    /**
     * Generates the templates.
     *
     * @param cfg      - the template configuration
     * @param template - the template to be generated
     * @return the generated template
     * @throws TemplateException
     * @throws IOException
     */
    private GeneratedTemplate generateAmwTemplateModel(Configuration cfg, TemplateDescriptorEntity template,
                                                       AmwTemplateModel model) throws TemplateException, IOException {
        // If the template has a target path and is
        // therefore not only a
        // nested template pattern, generate it!
        String targetPath = "";
        String fileContent = "";

        logStartTemplateGeneration(template);
        if (template.getName() != null) {
            if (template.getTargetPath() != null && !template.getTargetPath().isEmpty()) {

                // Process targetPath the name can contain properties as well
                Writer targetPathWriter = new StringWriter();

                freemarker.template.Template targetPathTemplate = cfg.getTemplate(template.getName()
                        + FILEPATH_PLACEHOLDER);
                targetPathTemplate.process(model, targetPathWriter);
                targetPathWriter.flush();
                targetPath = targetPathWriter.toString();
                cfg.clearTemplateCache();
                ((StringTemplateLoader) cfg.getTemplateLoader()).putTemplate(template.getName()
                        + FILEPATH_PLACEHOLDER, targetPath);
            }

            // Process file content
            Writer fileContentWriter = new StringWriter();
            freemarker.template.Template fileContentTemplate = cfg.getTemplate(template.getName());
            fileContentTemplate.process(model, fileContentWriter);
            fileContentWriter.flush();
            fileContent = fileContentWriter.toString();
            cfg.clearTemplateCache();
            ((StringTemplateLoader) cfg.getTemplateLoader()).putTemplate(template.getName(), fileContent);
        }
        // replace "escaping annotations" with value
        fileContent = fileContent.replace("@@{", "${");
        logFinishedTemplateGeneration(template);

        GeneratedTemplate generatedTemplate = new GeneratedTemplate(template.getName(), targetPath,
                fileContent);

        generatedTemplate.setTemplateEntity(template);
        return generatedTemplate;
    }

    public static Configuration getConfiguration(AMWTemplateExceptionHandler templateExceptionHandler) {
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_22);
        cfg.setTemplateExceptionHandler(templateExceptionHandler);
        cfg.setShowErrorTips(false);
        cfg.setLogTemplateExceptions(false);
        // because otherwise freemarker renders numbers by default like 1,000,000 http://freemarker.org/docs/app_faq.html#faq_number_grouping
        cfg.setNumberFormat("0.######");
        return cfg;
    }


    /**
     * Evaluates the given function and returns the result as String
     *
     * @param function
     * @param model
     * @param amwModelPreprocessExceptionHandler
     * @return
     * @throws IOException
     * @throws TemplateException
     */
    public String evaluateAmwFunction(AmwFunctionEntity function, AmwTemplateModel model, AmwModelPreprocessExceptionHandler amwModelPreprocessExceptionHandler) throws IOException, TemplateException {
        Configuration cfg = getConfiguration(amwModelPreprocessExceptionHandler);
        StringTemplateLoader loader = new StringTemplateLoader();

        String tempTemplateName = "_evaluateFunctionTemplate_";
        String tempTemplate = "<#include \"" + function.getName() + "\">${" + function.getName() + "()}";

        loader.putTemplate(tempTemplateName, tempTemplate);

        loader.putTemplate(function.getName(), function.getDecoratedImplementation());

        addGlobalFunctionTemplates(model.getGlobalFunctionTemplates(), loader);
        cfg.setTemplateLoader(loader);

        Writer fileContentWriter = new StringWriter();
        freemarker.template.Template fileContentTemplate = cfg.getTemplate(tempTemplateName);
        fileContentTemplate.process(model, fileContentWriter);
        fileContentWriter.flush();
        String result = fileContentWriter.toString();
        cfg.clearTemplateCache();

        return result;
    }

    /**
     * Evaluates a Property Value as a Template sometimes property Values may contain Freemarker strings lik ${env} and so on
     *
     * @param property
     * @param model
     * @return
     * @throws IOException
     * @throws TemplateException
     */
    public String evaluateStringPropertyAsTemplate(String property, AmwTemplateModel model) throws IOException, TemplateException {
        Configuration cfg = getConfiguration(model.getAmwModelPreprocessExceptionHandler());
        StringTemplateLoader loader = new StringTemplateLoader();

        String tempTemplateName = "_propertyEvaluation_";

        loader.putTemplate(tempTemplateName, property);

        addGlobalFunctionTemplates(model.getGlobalFunctionTemplates(), loader);

        cfg.setTemplateLoader(loader);

        Writer fileContentWriter = new StringWriter();
        freemarker.template.Template fileContentTemplate = cfg.getTemplate(tempTemplateName);
        fileContentTemplate.process(model, fileContentWriter);
        fileContentWriter.flush();
        String result = fileContentWriter.toString();
        cfg.clearTemplateCache();

        return result;
    }

    private void logFinishedTemplateGeneration(TemplateDescriptorEntity template) {
        log.finest("finished: " + template.toString());
    }

    private void logStartTemplateGeneration(TemplateDescriptorEntity template) {
        log.finest("started: " + template.toString());
    }

    private void logBeforeException(ParseException pe) {
        log.info("ParseException: ");
        log.severe(pe.getLocalizedMessage());
    }

}
