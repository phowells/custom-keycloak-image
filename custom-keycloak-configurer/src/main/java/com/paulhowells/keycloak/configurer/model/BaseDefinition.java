package com.paulhowells.keycloak.configurer.model;

import org.slf4j.Logger;

import java.util.*;

public abstract class BaseDefinition {

    public boolean isUnchanged(Object o, String parentName, Logger logger) {
        boolean result = true;

        if (o == null) {
            result = false;
            logger.info("{} has been removed", parentName);
        } else if (!this.getClass().equals(o.getClass())) {
            logger.info("{} has changed from {} to {}", parentName, this, o);
            result = false;
        }

        return result;
    }

    protected static boolean isUnchanged(Object o1, Object o2, String parentName, String propertyName, Logger logger) {

        return isUnchanged(o1, o2, parentName, propertyName, false, logger);
    }

    protected static boolean isUnchanged(Object o1, Object o2, String parentName, String propertyName, boolean sensitive, Logger logger) {

        String name;
        if (parentName == null) {
            name = propertyName;
        } else {
            name = String.format("%s.%s", parentName, propertyName);
        }

        return isUnchanged(o1, o2, name, sensitive, logger);
    }

    public static boolean isUnchanged(Object o1, Object o2, String propertyName, boolean sensitive, Logger logger) {
        boolean result;

        if (o1 == null && o2 == null) {
            result = true;
        } else if (o1 == null) {
            result = false;
            if (sensitive) {
                logger.info("{} has changed", propertyName);
            } else {
                logger.info("{} has changed from {} to {}", propertyName, o1, o2);
            }
        } else if (o2 == null) {
            result = false;
            if (sensitive) {
                logger.info("{} has changed", propertyName);
            } else {
                logger.info("{} has changed from {} to {}", propertyName, o1, o2);
            }
        } else if (o1 instanceof BaseDefinition) {

            result = ((BaseDefinition) o1).isUnchanged(o2, propertyName, logger);
        } else if (o1 instanceof List<?> c1) {
            result = true;

            List<?> c2 = (List<?>) o2;

            int size = Math.max(c1.size(), c2.size());

            for (int i = 0; i < size; ++i) {

                Object i1 = c1.size() > i ? c1.get(i) : null;
                Object i2 = c2.size() > i ? c2.get(i) : null;

                result = isUnchanged(i1, i2, String.format("%s[%s]", propertyName, i), sensitive, logger) & result;
            }
        } else if (o1 instanceof Map<?, ?> m1) {
            result = true;

            Map<?, ?> m2 = (Map<?, ?>) o2;

            Set<Object> keys = new HashSet<>();
            keys.addAll(m1.keySet());
            keys.addAll(m2.keySet());

            for (Object key:keys) {

                if ("managed-by".equals(key)) {
                    // We can skip the managed-by attribute because this gets set by the code
                } else {
                    Object i1 = m1.get(key);
                    Object i2 = m2.get(key);

                    result = isUnchanged(i1, i2, String.format("%s[%s]", propertyName, key), sensitive, logger) & result;
                }
            }
        } else {
            result = Objects.equals(o1, o2);

            if (!result) {
                logger.info("{} has changed from {} to {}", propertyName, o1, o2);
            }
        }

        return result;
    }
}
