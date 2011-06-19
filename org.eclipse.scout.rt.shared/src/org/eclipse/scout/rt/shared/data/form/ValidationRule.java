/*******************************************************************************
 * Copyright (c) 2010 BSI Business Systems Integration AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     BSI Business Systems Integration AG - initial API and implementation
 ******************************************************************************/
package org.eclipse.scout.rt.shared.data.form;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.eclipse.scout.commons.annotations.FormData;
import org.eclipse.scout.rt.shared.services.common.code.ICodeType;

/**
 * Method annotation on a client form field used in scout sdk in order to generate the validation rule map on the
 * FormData.
 * <p>
 * Scout SDK writes the value of every annotated method into the static map "validationRules" of the corresponding
 * FormFielData.
 * <p>
 * This annotation is only processed when a form data is autmatically managed by the scout sdk using the
 * {@link FormData} annotation.
 * <p>
 * Example for the form data field generated for a string field inside a form
 * 
 * <pre>
 * public class LastNameField extends AbstractStringField{
 *   ...
 * 
 *   protected boolean getConfiguredMandatory(){
 *     return true;
 *   }
 * 
 *   protected Integer getConfiguredMaxLength(){
 *     return 60;
 *   }
 * }
 * </pre>
 * 
 * <pre>
 * public class LastName extends AbstractValueFieldData&lt;String&gt;{
 *   ...
 *   public static final HashMap&lt;String,Object&gt; validationRules=new HashMap&lt;String,Object&gt;();
 *   static{
 *     validationRules.put("mandatory",true);
 *     validationRules.put("maxLength",60);
 *   }
 * }
 * </pre>
 * 
 * The two rule "put" lines were generated by the sdk due to the {@link ValidationRule} annotations on AbstractFormField
 * and AbstractStringField
 * <p>
 * The scout server (runtime) checks all inbound and outbound form datas and form fields according to their
 * validationRules defined in the shared FormData classes (central validation concept).
 * <p>
 * Custom validation rule names can freely be used (it's a String or a string constant).
 * <p>
 * Validation is implemented by either (a) overriding ServiceTunnelServlet#runServerJobTransactionWithDelegate with an
 * own DefaultTransactionDelegate subclass) that overrides the filterInput() method or (b) annotating service operation
 * methods with {@link InputValidation} and (optionally) {@link OutputValidation}.
 * <p>
 * When the sdk fails to create a rule for an annotated (directly or implicit by superclass) method to the created form
 * data it adds a javadoc entry specifying the fully qualified source method name and the keyword
 * "xxx ValidationRule.NotProcessed" that can be searched in the source code.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface ValidationRule {
  /**
   * the name of the rule, either any of the ValidationRule constants or a custom constant
   */
  String value();

  /**
   * When setting this annotation property, the scout sdk will not generate the validation rule value based on the
   * method return value but
   * use exactly this string as generated source code.
   * <p>
   * Example:
   * 
   * <pre>
   * @ValidationRule(value=ValidationRule.CODE_TYPE, generatedSourceCode="com.myproject.shared.services.code.StatusCodeType.class")
   * Class <? extends {@link ICodeType}> getConfiguredCodeType(){
   *   return ClientSpecificStatusCodeType.class;
   * }
   * </pre>
   */
  String generatedSourceCode() default "";

  /**
   * When setting skip to true, the scout sdk will not generate code for this validation rule at all.
   * Example:
   * 
   * <pre>
   * @ValidationRule(value=ValidationRule.CODE_TYPE, skip=true)
   * Class <? extends {@link ICodeType}> getConfiguredCodeType(){
   *   return ClientSpecificStatusCodeType.class;
   * }
   * </pre>
   */
  boolean skip() default false;

  /**
   * rule value type is {@link Boolean}
   * <p>
   * default rule packaged with scout
   */
  String MANDATORY = "mandatory";
  /**
   * rule value type is {@link Number}
   * <p>
   * default rule packaged with scout
   */
  String MIN_VALUE = "minValue";
  /**
   * rule value type is {@link Number}
   * <p>
   * default rule packaged with scout
   */
  String MAX_VALUE = "maxValue";
  /**
   * rule value type is {@link Integer}
   * <p>
   * default rule packaged with scout
   */
  String MIN_LENGTH = "minLength";
  /**
   * rule value type is {@link Integer}
   * <p>
   * default rule packaged with scout
   */
  String MAX_LENGTH = "maxLength";
  /**
   * rule value type is {@link Class}
   * <p>
   * default rule packaged with scout
   */
  String CODE_TYPE = "codeType";
  /**
   * rule value type is {@link Class}
   * <p>
   * default rule packaged with scout
   */
  String LOOKUP_CALL = "lookupCall";
}
