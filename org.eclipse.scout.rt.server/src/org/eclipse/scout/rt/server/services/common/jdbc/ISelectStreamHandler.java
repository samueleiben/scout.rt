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
package org.eclipse.scout.rt.server.services.common.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;

import org.eclipse.scout.commons.exception.ProcessingException;

public interface ISelectStreamHandler {

  void handleRow(Connection con, PreparedStatement stm, ResultSet rs, int rowIndex, List<SqlBind> values) throws ProcessingException;

  void finished(Connection con, PreparedStatement stm, ResultSet rs, int rowCount) throws ProcessingException;

}