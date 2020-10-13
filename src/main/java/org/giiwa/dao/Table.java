/*
 * Copyright 2015 JIHU, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/
package org.giiwa.dao;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * the {@code Mapping} Class used to annotate the Bean, define the
 * collection/table mapping with the Bean
 * 
 * <pre>
 * name, the table name
 * </pre>
 * 
 * @author joe
 *
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface Table {

	/**
	 * the table name.
	 *
	 * @return the table name
	 */
	String name() default X.EMPTY;

	/**
	 * specify the dbname
	 * 
	 * @return the db name
	 */
	String db() default Helper.DEFAULT;

	/**
	 * comment
	 * 
	 * @return
	 */
	String memo() default X.EMPTY;

	/**
	 * method list <br>
	 * e.g.: "list, create, edit"
	 * 
	 * @return
	 */
	String method() default X.EMPTY;

}
