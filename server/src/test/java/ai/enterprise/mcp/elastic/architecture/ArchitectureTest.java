package ai.enterprise.mcp.elastic.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Autowired;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.fields;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.GeneralCodingRules.NO_CLASSES_SHOULD_ACCESS_STANDARD_STREAMS;
import static com.tngtech.archunit.library.GeneralCodingRules.NO_CLASSES_SHOULD_USE_JAVA_UTIL_LOGGING;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

/**
 * Enforces the internal architecture rules A1–A7 from ADR-0003 §2.1. Run as plain
 * JUnit Jupiter tests (each rule {@code .check}ed against the imported production
 * classes) so they are unambiguously executed and counted by the build.
 * Release-blocking: a violation fails the build alongside the coverage gate.
 */
class ArchitectureTest {

    private static final JavaClasses PRODUCTION_CLASSES = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("ai.enterprise.mcp.elastic");

    // A1 — layering: dependencies point inward only.

    @Test
    void tool_does_not_depend_on_adapter() {
        ArchRule rule = noClasses().that().resideInAPackage("..tool..")
                .should().dependOnClassesThat().resideInAPackage("..adapter..");
        rule.check(PRODUCTION_CLASSES);
    }

    @Test
    void service_does_not_depend_on_tool_or_adapter() {
        ArchRule rule = noClasses().that().resideInAPackage("..service..")
                .should().dependOnClassesThat().resideInAnyPackage("..tool..", "..adapter..");
        rule.check(PRODUCTION_CLASSES);
    }

    @Test
    void domain_depends_on_nothing_outward() {
        ArchRule rule = noClasses().that().resideInAPackage("..domain..")
                .should().dependOnClassesThat()
                .resideInAnyPackage("..service..", "..tool..", "..adapter..", "..config..");
        rule.check(PRODUCTION_CLASSES);
    }

    @Test
    void port_depends_only_on_domain() {
        ArchRule rule = noClasses().that().resideInAPackage("..port..")
                .should().dependOnClassesThat()
                .resideInAnyPackage("..service..", "..tool..", "..adapter..", "..config..");
        rule.check(PRODUCTION_CLASSES);
    }

    // A2 — datastore encapsulation: Elasticsearch client types only inside ..adapter..

    @Test
    void elasticsearch_client_confined_to_adapter() {
        ArchRule rule = noClasses().that().resideOutsideOfPackage("..adapter..")
                .should().dependOnClassesThat()
                .resideInAnyPackage("co.elastic.clients..", "org.elasticsearch.client..")
                .as("Elasticsearch client types must be used only in the adapter package (ADR-0003 A2)");
        rule.check(PRODUCTION_CLASSES);
    }

    // A4 — tool-registration discipline.

    @Test
    void tools_live_in_tool_package() {
        ArchRule rule = methods().that().areAnnotatedWith(Tool.class)
                .should().beDeclaredInClassesThat().resideInAPackage("..tool..");
        rule.check(PRODUCTION_CLASSES);
    }

    @Test
    void tool_classes_named_Tools() {
        ArchRule rule = methods().that().areAnnotatedWith(Tool.class)
                .should().beDeclaredInClassesThat().haveSimpleNameEndingWith("Tools");
        rule.check(PRODUCTION_CLASSES);
    }

    // A5 — logging hygiene.

    @Test
    void no_java_util_logging() {
        NO_CLASSES_SHOULD_USE_JAVA_UTIL_LOGGING.check(PRODUCTION_CLASSES);
    }

    @Test
    void no_standard_streams() {
        NO_CLASSES_SHOULD_ACCESS_STANDARD_STREAMS.check(PRODUCTION_CLASSES);
    }

    // A6 — no package cycles.

    @Test
    void free_of_cycles() {
        ArchRule rule = slices().matching("ai.enterprise.mcp.elastic.(*)..").should().beFreeOfCycles();
        rule.check(PRODUCTION_CLASSES);
    }

    // A7 — constructor injection only (no field injection).

    @Test
    void no_field_injection() {
        ArchRule rule = fields().should().notBeAnnotatedWith(Autowired.class)
                .as("Use constructor injection, not @Autowired fields (ADR-0003 A7)");
        rule.check(PRODUCTION_CLASSES);
    }
}
