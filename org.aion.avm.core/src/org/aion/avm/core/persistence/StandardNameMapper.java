package org.aion.avm.core.persistence;

import java.util.HashSet;
import java.util.Set;
import org.aion.avm.NameStyle;
import org.aion.avm.core.ClassRenamer;
import org.aion.avm.core.ClassRenamer.ArrayType;
import org.aion.avm.core.ClassRenamerBuilder;
import org.aion.avm.core.types.CommonType;
import org.aion.avm.internal.PackageConstants;

public class StandardNameMapper implements IPersistenceNameMapper {
    private final ClassRenamer classRenamer;

    public StandardNameMapper(Set<String> postRenameSlashStyleUserDefinedClasses, boolean preserveDebuggability) {

        this.classRenamer = new ClassRenamerBuilder(NameStyle.DOT_NAME, preserveDebuggability)
            .loadPostRenameUserDefinedClasses(postRenameSlashStyleUserDefinedClasses)
            .loadPreRenameJclExceptionClasses(fetchPreRenameSlashStyleJclExceptions())
            .prohibitExceptionWrappers()
            .prohibitUnifyingArrayTypes()
            .build();
    }

    @Override
    public String getStorageClassName(String ourName) {
//        return ourName;
        return this.classRenamer.toPreRename(ourName);
    }

    @Override
    public String getInternalClassName(String storageClassName) {
//        return storageClassName;
        return this.classRenamer.toPostRename(storageClassName, ArrayType.PRECISE_TYPE);
    }

    private Set<String> fetchPreRenameSlashStyleJclExceptions() {
        Set<String> jclExceptions = new HashSet<>();

        for (CommonType type : CommonType.values()) {
            if (type.isShadowException) {
                jclExceptions.add(type.dotName.substring(PackageConstants.kShadowDotPrefix.length()).replaceAll("\\.", "/"));
            }
        }

        return jclExceptions;
    }
}
