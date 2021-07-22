package ch.puzzle.itc.mobiliar.business.utils.database;

import ch.puzzle.itc.mobiliar.business.database.control.EntityManagerProducerIntegrationTestImpl;
import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.changelog.ChangeSet;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.LiquibaseException;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.junit.Test;

import java.io.File;
import java.sql.DriverManager;
import java.util.List;

import static junit.framework.TestCase.fail;


public class LiquibaseChangeSetTest {


    private Database database;
    private java.sql.Connection conn = null;


    @Test
    public void shouldLoadInitialDataSetInEmptyDatabase() throws Exception {
        // given
        Class.forName("org.h2.Driver");
        conn = DriverManager.getConnection("jdbc:h2:mem:testdata", "sa", "");

        database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(conn));
        Liquibase liquibase = new Liquibase("liquibase/auto.db.changelog.xml", new ClassLoaderResourceAccessor(), database);

        //when
        liquibase.update(new Contexts(), new LabelExpression());

        liquibase.close();
        // then must not fail
        if(conn !=null) {
            conn.close();
        }
    }

    @Test
    public void shouldNotHaveAnyOpenChangeSets() throws Exception {
        // given
        File testDB = EntityManagerProducerIntegrationTestImpl.copyIntegrationTestDB("amwFileDbIntegrationOpenChangeSets.mv.db");
        conn = DriverManager.getConnection("jdbc:h2:file:" + testDB.getParent().toString() + "/amwFileDbIntegrationOpenChangeSets", "sa", "");
        database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(conn));
        Liquibase liquibase = new Liquibase("liquibase/auto.db.changelog.xml", new ClassLoaderResourceAccessor(), database);

        //when
        List<ChangeSet> unrunChangeSets = liquibase.listUnrunChangeSets(new Contexts(), new LabelExpression());

        // if there are open Changesets apply them and look for errors
        if(unrunChangeSets.size()>0){
            try {
                liquibase.update(new Contexts(), new LabelExpression());
            }catch (LiquibaseException e){
                fail("There are open Database Changesets on the local H2 Database, which fail when you apply them, you need to fix that and apply them afterwards with: AMW_db_scripts/update_h2_test_db.sh");
            }finally {
                if(conn !=null) {
                    conn.close();
                }
            }
            fail("There are open Database Changesets on the local H2 Database, that can be applied without error, run AMW_db_scripts/update_h2_test_db.sh to apply them directly");
        }

        liquibase.close();
        if(conn != null) {
            conn.close();
        }
    }

}
