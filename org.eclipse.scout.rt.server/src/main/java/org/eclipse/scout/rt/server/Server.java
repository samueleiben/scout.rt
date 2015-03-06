package org.eclipse.scout.rt.server;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.eclipse.scout.rt.platform.cdi.OBJ;

/**
 * An object (typically a service implementation) with this annotation creates a server transaction whenever an
 * operationis called.
 * <p>
 * Only valid if the object is obtained using {@link OBJ#one(Class)} with an interface argument
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface Server {
  Class<? extends IServerSession> value() default IServerSession.class;
}
