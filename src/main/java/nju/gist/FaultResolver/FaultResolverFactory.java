package nju.gist.FaultResolver;

import nju.gist.Common.Testcase;
import nju.gist.Tester.Checker;

import java.lang.reflect.Constructor;
import java.util.List;

public class FaultResolverFactory {
    public static <T extends AbstractFaultResolver> T createResolver(Class<T> clazz, Checker<?> checker, List<Testcase> Tpass, List<Testcase> Tfail) throws Exception {
        Class<?>[] parameterTypes = new Class<?>[]{
            Checker.class,
            List.class,
            List.class
        };

        Constructor<T> constructor = clazz.getDeclaredConstructor(parameterTypes);
        constructor.setAccessible(true);
        return constructor.newInstance(checker, Tpass, Tfail);
    }

}
