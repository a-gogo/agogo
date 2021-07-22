package ch.puzzle.itc.mobiliar.common.util;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.Getter;

import javax.xml.bind.annotation.XmlTransient;

@JsonSerialize(using = ConfigKeySerializer.class)
public enum ConfigKey {

    LOGS_PATH("amw.logsPath", "AMW_LOGSPATH", false),

    /** Age of logs to be deleted in minutes */
    LOGS_CLEANUP_AGE("amw.logsLeanupAge", "AMW_LOGSLEANUPAGE", Integer.valueOf(7*24*60).toString(), false),
    LOGS_CLEANUP_SCHEDULER_DISABLED("amw.logsCleanupSchedulerDisabled", "AMW_LOGSCLEANUPSCHEDULERDISABLED", "false", false),
    /** Path where the generator writes the files */
    GENERATOR_PATH("amw.generatorPath", "AMW_GENERATORPATH", false),
    /** Path where the generator writes the files for simulation modus */
    GENERATOR_PATH_SIMULATION("amw.generatorPath.simulation", "AMW_GENERATORPATH_SIMULATION", false),
    /** Path where the generator writes the files for test modus */
    GENERATOR_PATH_TEST("amw.generatorPath.test", "AMW_GENERATORPATH_TEST", false),
    LOG_RUNSCRIPT_OUTPUT_TO_SERVER_LOG("amw.logRunscriptOutputToServerLog", "AMW_LOG_RUNSCRIPT_OUTPUT_TO_SERVER_LOG", "false", false),
    MAIL_DOMAIN("amw.mailDomain", "AMW_MAILDOMAIN", false),
    DELIVER_MAIL("amw.deliverMail", "AMW_DELIVERMAIL", false),
    ENCRYPTION_KEY("amw.encryptionKey", "AMW_ENCRYPTIONKEY", true),
    LOGOUT_URL("amw.logoutUrl", "AMW_LOGOUTURL", false),
    STM_PATH("amw.stmpath", "AMW_STMPATH", false),
    STM_REPO("amw.stmrepo", "AMW_STMREPO", false),
    TEST_RESULT_PATH("amw.testResultPath", "AMW_TESTRESULTPATH", false),
    DEPLOYMENT_IN_PROGRESS_TIMEOUT("amw.deploymentInProgressTimeout", "AMW_DEPLOYMENTINPROGRESSTIMEOUT", "3600", false),
    PREDEPLOYMENT_IN_PROGRESS_TIMEOUT("amw.predeploymentInProgressTimeout", "AMW_PREDEPLOYMENTINPROGRESSTIMEOUT", "7200", false),
    DEPLOYMENT_PROCESSING_AMOUNT_PER_RUN("amw.deploymentProcessingAmountPerRun", "AMW_DEPLOYMENTPROCESSINGAMOUNTPERRUN", "5", false),
    DEPLOYMENT_SIMULATION_AMOUNT_PER_RUN("amw.deploymentSimulationAmountPerRun", "AMW_DEPLOYMENTSIMULATIONAMOUNTPERRUN", "5", false),
    DEPLOYMENT_PREDEPLOYMENT_AMOUNT_PER_RUN("amw.deploymentPredeploymentAmountPerRun", "AMW_DEPLOYMENTPREDEPLOYMENTAMOUNTPERRUN", "5", false),
    DEPLOYMENT_SCHEDULER_DISABLED("amw.deploymentSchedulerDisabled", "AMW_DEPLOYMENTSCHEDULERDISABLED", "false", false),
    DEPLOYMENT_CLEANUP_SCHEDULER_DISABLED("amw.deploymentCleanupSchedulerDisabled", "AMW_DEPLOYMENTCLEANUPSCHEDULERDISABLED", "false", false),
    /** Age of folders to be deleted in minutes */
    DEPLOYMENT_CLEANUP_AGE("amw.deploymentCleanupAge", "AMW_DEPLOYMENTCLEANUPAGE", "240", false),
    VM_DETAIL_URL("amw.vmDetailUrl", "AMW_VMDETAILURL", false),
    VM_URL_PARAM("amw.vmUrlParam", "AMW_VMURLPARAM", false),
    CSV_SEPARATOR("amw.csvSeparator", "AMW_CSVSEPARATOR", ";", false),
    LOCAL_ENV("amw.localEnv", "AMW_LOCALENV", "Local", false),
    PROVIDABLE_SOFTLINK_RESOURCE_TYPES("amw.providableSoftlinkResourceTypes", "AMW_PROVIDABLESOFTLINKRESOURCETYPES", false),
    CONSUMABLE_SOFTLINK_RESOURCE_TYPES("amw.consumableSoftlinkResourceTypes", "AMW_CONSUMABLESOFTLINKRESOURCETYPES", false),
    EXTERNAL_RESOURCE_BACKLINK_SCHEMA("amw.externalResourceBacklinkSchema", "AMW_EXTERNALRESOURCEBACKLINKSCHEMA", false),
    EXTERNAL_RESOURCE_BACKLINK_HOST("amw.externalResourceBacklinkHost", "AMW_EXTERNALRESOURCEBACKLINKHOST", false),
    /** Create not Existent Directory Structure */
    CREATE_NOT_EXISTING_DIRECTORIES_ON_STARTUP("amw.createNotExistingDirectoriesOnStartUp", "AMW_CREATENOTEXISTINGDIRECTORIESONSTARTUP", "false", false),
    /** Feature toggles */
    FEATURE_DISABLE_ANGULAR_GUI("amw.feature.disableAngularGui","AMW_FEATURE_DISABLEANGULARGUI","false", false),
    FEATURE_DISABLE_ANGULAR_DEPLOYMENT_GUI("amw.feature.disableAngularDeploymentGui", "AMW_FEATURE_DISABLEANGULARDEPLOYMENTGUI", "false", false),
    LIQUIBASE_DATASOURCE_JNDI("amw.liquibaseDatasourceJndi", "AMW_LIQUIBASE_DATASOURCE_JNDI", "java:jboss/datasources/amwLiquibaseDS", false);

    @Getter
    private String value;
    @Getter
    private String envName;
    @Getter
    private String defaultValue;
    @Getter
    @XmlTransient
    @JsonIgnore
    private boolean secretValue;

    ConfigKey(String value, String envName, boolean secretValue) {
        this.value = value;
        this.envName = envName;
        this.secretValue = secretValue;
    }

    ConfigKey(String value, String envName, String defaultValue, boolean secretValue) {
        this(value, envName, secretValue);
        this.defaultValue = defaultValue;
    }

}
