package io.spartan.jenkinslib

import io.spartan.jenkinslib.helpers.BaseHelper

import java.lang.reflect.Constructor

class JenkinsLibInjector implements Serializable {
    Script ctx
    JenkinsLibProperties libProperties
    Map<Class<? extends BaseHelper>, ? extends BaseHelper> helpers

    JenkinsLibInjector(ctx, JenkinsLibProperties libProperties) {
        this.ctx = ctx
        this.libProperties = libProperties
        helpers = new HashMap<>()
    }

    def <T extends BaseHelper> T getHelperForClass(Class<T> clazz) {
        // for some classes using Map#computeIfAbsent is not working properly
        if (!helpers.get(clazz)) {
            helpers.put(clazz, instantiateHelperForClass(clazz))
        }
        helpers.get(clazz) as T
    }

    private <T extends BaseHelper> T instantiateHelperForClass(Class<T> clazz) {
        instantiateLibraryClass clazz
    }

    private <T> T instantiateLibraryClass(Class<T> clazz) {
        if (clazz.declaredConstructors.size() == 0) {
            ctx.error "no constructor found for class '$clazz.name'"
        } else if (clazz.declaredConstructors.size() > 1) {
            ctx.error "multiple constructors found for class '$clazz.name'"
        }

        def constructor = clazz.declaredConstructors[0] as Constructor<T>
        def constructorParameters = constructor.parameterTypes.collect { parameterType ->
            if (Script.isAssignableFrom(parameterType)) {
                ctx
            } else if (JenkinsLibProperties.isAssignableFrom(parameterType)) {
                libProperties
            } else if (BaseHelper.isAssignableFrom(parameterType)) {
                getHelperForClass(parameterType as Class<? extends BaseHelper>)
            } else {
                ctx.error "unsupported parameter type '$clazz.name'"
            }
        }

        constructor.newInstance(*constructorParameters)
    }
}
