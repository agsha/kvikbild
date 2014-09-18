package sharath;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.sql.SQLException;
import java.util.HashMap;

/**
 * @author sgururaj
 */
public class ResourceTask {
    private static final Logger log = LogManager.getLogger(ResourceTask.class);
    CimModule module;
    Utils utils;
    External ext;

    private ResourceTask(CimModule module, Utils utils, External ext) {
        this.module = module;
        this.utils = utils;
        this.ext = ext;
    }


    public void updateResources() throws IOException {
        ext.mkdir(module.destResource);
        ext.mkdir(module.destTestResource);
        final HashMap<String, FileTime> modifiedTimes = new HashMap<>(3000);
        ext.walkFileTree(ImmutableList.of(module.destResource, module.destTestResource),
            new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    modifiedTimes.put(file.toString(), attrs.lastModifiedTime());
                    return FileVisitResult.CONTINUE;
                }
            });
        ext.walkFileTree(ImmutableList.of(module.srcResource, module.srcTestResource),
            new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    Path tgtDir = Paths.get(utils.toTargetResource(dir));
                    if(!Files.exists(tgtDir)){
                        Files.createDirectory(tgtDir);
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    String target = utils.toTargetResource(file);
                    if(!modifiedTimes.containsKey(target) || modifiedTimes.get(target).compareTo(attrs.lastModifiedTime())<0) {
                        log.info("updating stale resource: "+file);
                        Files.copy(file, Paths.get(target));
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
    }

    static class Factory {
        External ext;
        private Utils.Factory utilFactory;
        private CimModule.AllModules allModules;

        @Inject
        Factory(External ext, Utils.Factory utilFactory, CimModule.AllModules allModules) {
            this.ext = ext;
            this.utilFactory = utilFactory;
            this.allModules = allModules;
        }

        public ResourceTask createCoreResourceTask() throws SQLException {
            return create(allModules.forName("core"));
        }
        public ResourceTask create(CimModule module) throws SQLException {
            return new ResourceTask(module, utilFactory.createCoreUtils(), ext);
        }
    }
}
