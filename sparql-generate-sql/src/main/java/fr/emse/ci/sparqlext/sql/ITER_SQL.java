/*
 * Copyright 2016 Ecole des Mines de Saint-Etienne.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package fr.emse.ci.sparqlext.sql;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.jena.sparql.expr.ExprEvalException;
import org.apache.jena.sparql.expr.ExprList;
import org.apache.jena.sparql.expr.NodeValue;

import org.apache.jena.sparql.expr.nodevalue.NodeValueDouble;

import org.apache.jena.sparql.expr.nodevalue.NodeValueFloat;
import org.apache.jena.sparql.expr.nodevalue.NodeValueInteger;
import org.apache.jena.sparql.expr.nodevalue.NodeValueString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.emse.ci.sparqlext.SPARQLExt;
import fr.emse.ci.sparqlext.iterator.IteratorFunctionBase2;

/**
 * @author Omar Qawasmeh, Maxime Lefrançois
 * 
 * @organization Ecole des Mines de Saint Etienne
 */
public class ITER_SQL extends IteratorFunctionBase2 {

	/**
	 * The logger.
	 */
	private static final Logger LOG = LoggerFactory.getLogger(ITER_SQL.class);

	/**
	 * The SPARQL function URI.
	 */
	public static final String URI = SPARQLExt.ITER + "SQL";
	public static final Properties properties = new Properties();

	static {
		properties.setProperty("user", "root");
		properties.setProperty("password", "omar1234");
		properties.setProperty("serverTimezone", "UTC");
	}

	public static Properties getProperties() {
		return properties;
	}

	@Override
	public List<List<NodeValue>> exec(NodeValue nodeSQL, NodeValue querySQL) {

		if (nodeSQL == null) {
			LOG.debug("Must have two arguments, the URI to the data base and the SQL query");
			throw new ExprEvalException("Must have two arguments, the URI to the data base and the SQL query");
		}
		if (querySQL == null) {
			LOG.debug("Must have two arguments, the URI to the data base and the SQL query");
			throw new ExprEvalException("Must have two arguments, the URI to the data base and the SQL query");
		}

		LOG.trace("Executing SQL with variables: the data base at URI: " + nodeSQL + "\t with query:\t" + querySQL);

		try (Connection connectionSQL = getConnection(nodeSQL)) {
			LOG.trace("Connected successfuly to " + nodeSQL);
			return getListSQL(connectionSQL, querySQL);
		} catch (Exception ex) {
			LOG.warn("Can not connect to the data base", ex);
			throw new ExprEvalException("Can not connect to the data base", ex);
		}
	}

	public static Connection getConnection(NodeValue sql) throws Exception {
		if (sql.isIRI()) {
			String sqlPath = sql.asNode().getURI();
			try {
				Class.forName("com.mysql.cj.jdbc.Driver");
				Connection conn = DriverManager.getConnection(sqlPath, properties);
				return conn;
			} catch (Exception e) {
				LOG.warn("Can not connect to the data base", e);
				throw new ExprEvalException("Can not connect to the data base", e);
			}
		} else {
			String message = String.format("First argument must be a URI to the SQL data base");
			LOG.warn(message);
			throw new ExprEvalException(message);
		}
	}

	public static List<List<NodeValue>> getListSQL(Connection conn, NodeValue querySQL) {
		List<List<NodeValue>> nodeValuesAllRows = new ArrayList<>();
		if (!querySQL.isString()) {
			String message = String.format("Second argument (the query) must be a String");
			LOG.warn(message);
			throw new ExprEvalException(message);
		} else {
			LOG.trace("Exceuting the quey: " + querySQL.asString());
			try (PreparedStatement ps = conn.prepareStatement(querySQL.asString()); ResultSet rs = ps.executeQuery()) {
				ResultSetMetaData rsmd = rs.getMetaData();
				int columnCount = rsmd.getColumnCount();
				while (rs.next()) {
					List<NodeValue> listRow = new ArrayList<>();
					int i = 1;
					while (i <= columnCount) {
						NodeValue nv = getNodeValueForCell(rs, rsmd, i++);
						listRow.add(nv);
					}
					nodeValuesAllRows.add(listRow);

				}
			} catch (SQLException e) {
				LOG.warn(e.getMessage());
				throw new ExprEvalException(e);
			}
		}

		return nodeValuesAllRows;
	}

	private static NodeValue getNodeValueForCell(ResultSet rs, ResultSetMetaData rsmd, int i) throws SQLException {
		switch (rsmd.getColumnType(i)) {
		case Types.ARRAY:
			return new NodeValueString(rs.getString(i));
		case Types.INTEGER:
		case Types.BIGINT:
			return new NodeValueInteger(rs.getInt(i));
		case Types.NULL:
			return null;
		case Types.DOUBLE:
			return new NodeValueDouble(rs.getDouble(i));
		case Types.FLOAT:
			return new NodeValueFloat(rs.getFloat(i));
		case Types.VARCHAR:
			return new NodeValueString(rs.getString(i));
		case Types.TIMESTAMP:
			DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
			Timestamp date = rs.getTimestamp(i);
			System.out.println("DATE: " + date);
			dateFormat.format(date);
			System.out.println(dateFormat.format(date));
			return new NodeValueString(dateFormat.format(date));

		default:
			return new NodeValueString(rs.getString(i));
		}

	}

	@Override
	public void checkBuild(ExprList args) {
	}

}
