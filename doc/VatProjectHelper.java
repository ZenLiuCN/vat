import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * VAT Project Management
 * 1. usage: `java VatProjectHelper.java`
 * 2. change with shebang type:
 * + add line `#! /opt/jdk/java --source 21
 * + remove extension `.java`
 * + execute as a shell script
 */
public class VatProjectHelper {

    private static final Scanner scanner = new Scanner(System.in);
    private static final Path ROOT = Paths.get(".");
    private static final String VAT_GROUP = "io.github.zenliucn.vertx.vat";

    public static void main(String[] args) {
        if (Runtime.version().feature() < 21) {
            System.err.println("Error: JDK 21+ required.");
            System.exit(1);
        }

        while (true) {
            boolean initialized = Files.exists(ROOT.resolve("pom.xml"));
            printMenu(initialized);
            String choice = scanner.nextLine();
            try {
                switch (choice) {
                    case "0" -> {if (!initialized) initProject();}
                    case "1" -> {if (initialized) addDomain();}
                    case "2" -> {if (initialized) addNode();}
                    case "3" -> {return;}
                }
            } catch (Exception e) {
                System.err.println("Error: " + e.getMessage());
            }
        }
    }

    private static void initProject() throws IOException {
        String gid = prompt("GroupId", "com.example");
        String aid = prompt("ArtifactId (Project Prefix)", "my-app");

        String envVer = System.getenv("VAT_VERSION");
        String ver = (envVer != null && !envVer.isBlank()) ? envVer : "1.0-SNAPSHOT";
        System.out.println("VAT Version: " + ver);

        IO.write(ROOT.resolve("pom.xml"), Templates.rootPom(gid, aid, ver));
        IO.write(ROOT.resolve(".gitignore"), Templates.gitignore());
        System.out.println("√ Root project initialized.");
    }

    private static void addDomain() throws IOException {
        String gid = PomManager.fetchTag("groupId");
        String aid = PomManager.fetchTag("artifactId");
        String domain = prompt("Domain Name (e.g., chat-service)", null);

        String pkg = Strings.toPackage(aid) + "." + Strings.toPackage(domain);
        String className = Strings.toClassName(domain);
        Path domainDir = ROOT.resolve(domain);

        IO.write(domainDir.resolve("pom.xml"), Templates.domainAggregator(gid, aid, Strings.toPackage(aid+"-"+domain),domain));

        Path apiPath = domainDir.resolve(domain + "-api");
        IO.write(apiPath.resolve("pom.xml"),
                 Templates.domainApiPom(gid,aid+"-"+domain , Strings.toPackage(aid + "." + domain + ".api")));
        IO.writeApiSource(apiPath, pkg, className);

        Path implPath = domainDir.resolve(domain + "-domain");
        IO.write(implPath.resolve("pom.xml"),
                 Templates.domainImplPom(gid, aid+"-"+domain, Strings.toPackage(aid+ "." + domain + ".domain") ));
        IO.createImplSource(implPath, pkg, className);

        PomManager.registerModule(domain);
        PomManager.addManagedDependency(gid, aid+"-"+domain + "-api");
        PomManager.addManagedDependency(gid, aid+"-"+domain + "-domain");

        System.out.println("√ Domain [" + domain + "] created. Package: " + pkg);
    }

    private static void addNode() throws IOException {
        String gid = PomManager.fetchTag("groupId");
        String aid = PomManager.fetchTag("artifactId");
        String node = prompt("Node Name", "gateway-node");

        Path nodePath = ROOT.resolve(node);
        IO.write(nodePath.resolve("pom.xml"), Templates.nodePom(gid, aid, node, Strings.toPackage(aid+ "." + node)) );
        Files.createDirectories(nodePath.resolve("src/main/resources"));

        PomManager.registerModule(node);
        System.out.println("√ Node created.");
    }

    // --- XML Utilities ---

    static class PomManager {
        static String fetchTag(String tag) throws IOException {
            String xml = Files.readString(ROOT.resolve("pom.xml"));
            Matcher m = Pattern.compile("<" + tag + ">(.*?)</" + tag + ">").matcher(xml);
            return m.find() ? m.group(1).trim() : "";
        }

        static void registerModule(String path) throws IOException {
            updateBlock("</modules>", "        <module>%s</module>".formatted(path), "<modules>");
        }

        static void addManagedDependency(String gid, String aid) throws IOException {
            String entry = """
                            <dependency>
                                <groupId>%s</groupId>
                                <artifactId>%s</artifactId>
                                <version>${project.version}</version>
                            </dependency>
                    """.formatted(gid, aid);
            updateBlock("</dependencies>", entry, "<dependencyManagement>");
        }

        private static void updateBlock(String closingTag, String entry, String context) throws IOException {
            Path path = ROOT.resolve("pom.xml");
            List<String> lines = Files.readAllLines(path);
            if (lines.stream().anyMatch(l -> l.contains(entry.trim()))) return;

            List<String> output = new ArrayList<>();
            boolean insideContext = false;
            boolean added = false;

            for (String line : lines) {
                if (line.contains(context)) insideContext = true;
                // Only add if we are inside the correct context (e.g. dependencyManagement)
                if (insideContext && line.contains(closingTag) && !added) {
                    output.add(entry);
                    added = true;
                }
                output.add(line);
            }
            Files.write(path, output);
        }
    }

    // --- Templates ---

    static class Templates {
        static String gitignore() {
            return """
                    target/
                    .idea/
                    .vscode/
                    *.iml
                    .DS_Store
                    """;
        }

        static String rootPom(String gid, String aid, String ver) {
            return """
                    <project xmlns="http://maven.apache.org/POM/4.0.0">
                        <modelVersion>4.0.0</modelVersion>
                        <groupId>%s</groupId>
                        <artifactId>%s</artifactId>
                        <version>1.0-SNAPSHOT</version>
                        <packaging>pom</packaging>
                        <parent>
                            <groupId>io.github.zenliucn.vertx</groupId>
                            <artifactId>vat</artifactId>
                            <version>%s</version>
                        </parent>
                        <properties>
                            <module.name>%s</module.name>
                        </properties>
                        <dependencyManagement>
                            <dependencies>
                            </dependencies>
                        </dependencyManagement>
                        <modules>
                        </modules>
                    </project>
                    """.formatted(gid, aid, ver, aid);
        }

        static String domainAggregator(String gid, String pAid,String mod, String domain) {
            return """
                    <project>
                        <modelVersion>4.0.0</modelVersion>
                        <parent>
                            <groupId>%1$s</groupId>
                            <artifactId>%2$s</artifactId>
                            <version>1.0-SNAPSHOT</version>
                        </parent>
                        <artifactId>%2$s-%4$s</artifactId>
                        <packaging>pom</packaging>
                        <properties>
                            <module.name>%3$s</module.name>
                        </properties>
                        <modules>
                            <module>%4$s-api</module>
                            <module>%4$s-domain</module>
                        </modules>
                    </project>
                    """.formatted(gid, pAid, mod,domain);
        }

        static String domainApiPom(String gid, String pAid,  String modName) {
            return """
                    <project>
                        <modelVersion>4.0.0</modelVersion>
                        <parent>
                            <groupId>%1$s</groupId>
                            <artifactId>%2$s</artifactId>
                            <version>1.0-SNAPSHOT</version>
                        </parent>
                        <artifactId>%2$s-api</artifactId>
                        <properties>
                            <module.name>%3$s</module.name>
                        </properties>
                    </project>
                    """.formatted(gid, pAid, modName);
        }

        static String domainImplPom(String gid, String domain, String modName) {
            return """
                    <project>
                        <modelVersion>4.0.0</modelVersion>
                        <parent>
                            <groupId>%1$s</groupId>
                            <artifactId>%2$s</artifactId>
                            <version>1.0-SNAPSHOT</version>
                        </parent>
                        <artifactId>%2$s-domain</artifactId>
                        <properties>
                            <module.name>%3$s</module.name>
                        </properties>
                        <dependencies>
                            <dependency>
                                <groupId>%1$s</groupId>
                                <artifactId>%2$s-api</artifactId>
                            </dependency>
                        </dependencies>
                    </project>
                    """.formatted(gid, domain,modName);
        }

        static String nodePom(String gid, String pAid, String aid, String modName) {
            return """
                    <project>
                        <modelVersion>4.0.0</modelVersion>
                        <parent>
                            <groupId>%s</groupId>
                            <artifactId>%s</artifactId>
                            <version>1.0-SNAPSHOT</version>
                        </parent>
                        <artifactId>%s</artifactId>
                        <properties>
                            <module.name>%s</module.name>
                            <node.shade>true</node.shade>
                        </properties>
                        <dependencies>
                            <dependency>
                                <groupId>%s</groupId>
                                <artifactId>core</artifactId>
                            </dependency>
                            <dependency>
                                <groupId>%s</groupId>
                                <artifactId>runtime-node</artifactId>
                            </dependency>
                        </dependencies>
                    </project>
                    """.formatted(gid, pAid, aid, modName, VAT_GROUP, VAT_GROUP);
        }

        static String apiJava(String pkg, String cls) {
            return """
                    package %s.api;
                    
                    import io.vertx.core.Future;
                    import io.vertx.core.json.JsonObject;
                    import org.jspecify.annotations.Nullable;
                    import org.jspecify.annotations.NullMarked;
                    import vat.api.*;
                    import vat.api.meta.*;
                    
                    @Enhance
                    @NullMarked
                    @Describe("DomainName")
                    public interface %2$s extends Activities {
                        //region domain objects
                        // Actor example
                        // @Enhance
                        // @Describe(value = "name")
                        // @Identity.Refer(domain = %2$s.class)
                        // @Table("TABLE_NAME")
                        // interface User extends Actor.Base {
                        //     @Describe("PROPERTY_NAME")
                        //     JsonObject profile();
                        // }
                    
                        // Ability example
                        // @Enhance
                        // @Describe(value = "ABILITY_NAME")
                        // @Identity.Refer(value = "certIdentity", domain = %2$s.class)
                        // @Table("ABILITY_TABLE")
                        // interface Certificate extends Record.Base {
                        //     @Describe("PROPERTY_NAME")
                        //     @Identity.Reference(value = User.class, provider = Users.class)
                        //     @Column(indexed = {"user"})
                        //     long user();
                        //     @Describe("_USERS_CERTIFICATE_IDENTIFIER")
                        //     @Column(size = 128)
                        //     String identifier();
                        // }
                    
                        //endregion
                    
                        //region domain actions
                    
                        // Storage action example
                        // @Describe("_USERS_ACT_USER_IDENTITY")
                        // @Access
                        // Future<Optional<User>> identity(long id);
                    
                        //endregion
                    
                        @Enhance
                        interface Context extends %2$s, Domain.Context {
                            // store example
                            // @Storage("/schema/users/user")
                            // default Store<User> users(@Nullable SqlConnection tx) {
                            //     throw new IllegalStateException("Not Implemented");
                            // }
                    
                            // configuration example
                            // @Config
                            // default boolean debug() {
                            //         return false;
                            // }
                    
                            // domain error example
                            // @Errors
                            // default DomainError alreadyRegistered() {
                            //     return DomainError.User.badRequestNotify("user is registered");
                            // }
                        }
                    }
                    """.formatted(pkg, cls);
        }

        static String implJava(String pkg, String cls) {
            return """
                    package %s.domain;
                    
                    import %s.api.%sDomain;
                    import com.google.auto.service.AutoService;
                    import org.jspecify.annotations.NullMarked;
                    import io.vertx.core.Vertx;
                    import vat.api.Activities;
                    import vat.api.meta.Activity;
                    
                    @AutoService(Activities.class)
                    @Activity(mode = Activity.Mode.DOMAIN,auto=true)
                    @NullMarked
                    public class %3$sImpl extends %3$sDomain<%3$sImpl> {
                        /// SPI constructor change to match super constructor.
                         public %3$sImpl() {
                            super();
                         }
                         /// Verticle Factory constructor. Change to match super constructor.
                         public %3$sImpl(Vertx vertx, String address) {
                            super(vertx,address/* ... */);
                         }
                         @Override
                         protected %2$sImpl _this() {
                             return this;
                         }
                    }
                    """.formatted(pkg, pkg, cls);
        }
    }

    static class IO {
        static void write(Path path, String content) throws IOException {
            if (Files.exists(path)) return;
            if (path.getParent() != null) Files.createDirectories(path.getParent());
            Files.writeString(path, content);
            System.out.println("  + " + path);
        }

        static void writeApiSource(Path path, String pkg, String cls) throws IOException {
            Path file = path.resolve("src/main/java/" + pkg.replace(".", "/") + "/api/" + cls + ".java");
            write(file, Templates.apiJava(pkg, cls));
            write(path.resolve(".enhance"), """
                    codegen {
                        ddl {
                            # mysql:true # for use mysql
                            # postgres:true # for use postgres
                        }
                    }
                    """);
        }

        static void createImplSource(Path path, String pkg, String cls) throws IOException {
            Path file = path.resolve("src/main/java/" + pkg.replace(".", "/") + "/domain/" + cls + "Impl.java");
            write(file, Templates.implJava(pkg, cls));
        }
    }

    static class Strings {
        static String toPackage(String s) {
            return s.replace("-", ".").toLowerCase();
        }

        static String toClassName(String domain) {
            String clean = domain.replaceAll("[^a-zA-Z0-9]", "");
            return clean.isEmpty() ? "Default" : clean.substring(0, 1).toUpperCase() + clean.substring(1);
        }
    }

    private static String prompt(String msg, String def) {
        System.out.print(msg + (def != null ? " [" + def + "]: " : ": "));
        String in = scanner.nextLine().trim();
        return in.isEmpty() ? def : in;
    }

    private static void printMenu(boolean init) {
        System.out.println("\n--- VAT HELPER ---");
        if (!init) System.out.println("0. Initialize Project");
        else {
            System.out.println("1. Add Domain Module");
            System.out.println("2. Add Startup Node");
        }
        System.out.println("3. Exit");
        System.out.print("Choice: ");
    }
}