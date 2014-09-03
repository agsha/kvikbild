package sharath;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.HashMap;

/**
 * @author sgururaj
 */
public class ResourceTask {
    private static final Logger log = Logger.getLogger(ResourceTask.class);
    Utils.CimModule module;
    Utils utils;
    External ext;

    private ResourceTask(Utils.CimModule module, Utils utils, External ext) {
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
        private Utils.CimModule coreModule;

        @Inject
        Factory(External ext, Utils.Factory utilFactory, @Named("core")Utils.CimModule coreModule) {
            this.ext = ext;
            this.utilFactory = utilFactory;
            this.coreModule = coreModule;
        }

        public ResourceTask createCoreResourceTask() {
            return create(coreModule);
        }
        public ResourceTask create(Utils.CimModule module) {
            return new ResourceTask(module, utilFactory.createCoreUtils(), ext);
        }
    }
}
