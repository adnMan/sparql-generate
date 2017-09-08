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
package com.github.thesmartenergy.sparql.generate.api;

import com.github.thesmartenergy.sparql.generate.jena.SPARQLGenerate;
import com.github.thesmartenergy.sparql.generate.jena.engine.PlanFactory;
import com.github.thesmartenergy.sparql.generate.jena.engine.RootPlan;
import com.google.gson.Gson;
import com.google.gson.internal.StringMap;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.spi.json.JacksonJsonProvider;
import com.jayway.jsonpath.spi.json.JsonProvider;
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider;
import com.jayway.jsonpath.spi.mapper.MappingProvider;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import org.apache.commons.io.IOUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.system.stream.Locator;
import org.apache.jena.riot.system.stream.StreamManager;

/**
 *
 * @author Maxime Lefrançois <maxime.lefrancois at emse.fr>
 */
@Path("/transform")
public class Transform extends HttpServlet {

    private static final Logger LOG = Logger.getLogger(Transform.class.getSimpleName());

    static {
        Configuration.setDefaults(new Configuration.Defaults() {

            private final JsonProvider jsonProvider = new JacksonJsonProvider();
            private final MappingProvider mappingProvider
                    = new JacksonMappingProvider();

            @Override
            public JsonProvider jsonProvider() {
                return jsonProvider;
            }

            @Override
            public MappingProvider mappingProvider() {
                return mappingProvider;
            }

            @Override
            public Set<Option> options() {
                return EnumSet.noneOf(Option.class);
            }
        });
    }

    @GET
    @Produces("text/turtle")
    public Response doGetTurtle(
            @Context Request r,
            @Context HttpServletRequest request,
            @DefaultValue("") @QueryParam("query") String query,
            @DefaultValue("") @QueryParam("queryurl") String queryurl,
            @DefaultValue("") @QueryParam("documentset") String documentset) throws IOException {
        return doTransform(r, request, query, queryurl, documentset, "TTL", "ttl");
    }

    @POST
    @Produces("text/turtle")
    public Response doPost(
            @Context Request r,
            @Context HttpServletRequest request,
            @DefaultValue("") @FormParam("query") String query,
            @DefaultValue("") @FormParam("queryurl") String queryurl,
            @DefaultValue("") @FormParam("documentset") String documentset) throws IOException {
        return doTransform(r, request, query, queryurl, documentset, "TTL", "ttl");
    }

    @GET
    @Produces("application/rdf+xml;q=0.9")
    public Response doGetTurtleRDFXML(
            @Context Request r,
            @Context HttpServletRequest request,
            @DefaultValue("") @QueryParam("query") String query,
            @DefaultValue("") @QueryParam("queryurl") String queryurl,
            @DefaultValue("") @QueryParam("documentset") String documentset) throws IOException {
        return doTransform(r, request, query, queryurl, documentset, "RDFXML", "rdf");
    }

    @POST
    @Produces("application/rdf+xml;q=0.9")
    public Response doPostRDFXML(
            @Context Request r,
            @Context HttpServletRequest request,
            @DefaultValue("") @FormParam("query") String query,
            @DefaultValue("") @FormParam("queryurl") String queryurl,
            @DefaultValue("") @FormParam("documentset") String documentset) throws IOException {
        return doTransform(r, request, query, queryurl, documentset, "RDFXML", "rdf");
    }

    @GET
    @Produces("application/ld+json;q=0.8,*/*;q=0.1")
    public Response doGetTurtleJSONLD(
            @Context Request r,
            @Context HttpServletRequest request,
            @DefaultValue("") @QueryParam("query") String query,
            @DefaultValue("") @QueryParam("queryurl") String queryurl,
            @DefaultValue("") @QueryParam("documentset") String documentset) throws IOException {
        return doTransform(r, request, query, queryurl, documentset, "JSON-LD", "jsonld");
    }

    @POST
    @Produces("application/ld+json;q=0.8,*/*;q=0.1")
    public Response doPostJSONLD(
            @Context Request r,
            @Context HttpServletRequest request,
            @DefaultValue("") @FormParam("query") String query,
            @DefaultValue("") @FormParam("queryurl") String queryurl,
            @DefaultValue("") @FormParam("documentset") String documentset) throws IOException {
        return doTransform(r, request, query, queryurl, documentset, "JSON-LD", "jsonld");
    }

    private Response doTransform(Request r, HttpServletRequest request, String query, String queryurl, String documentset, String lang, String ext) {
        if (query.equals("") && queryurl.equals("")) {
            return Response.status(Response.Status.BAD_REQUEST).entity("At least one of query or queryurl query parameters must be set.").build();
        }
        if (!queryurl.equals("") && !query.equals("")) {
            return Response.status(Response.Status.BAD_REQUEST).entity("At most one of query or queryurl query parameters must be set.").build();
        }

        try {
            if (!queryurl.equals("")) {
                URL url = new URL(queryurl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                query = IOUtils.toString(conn.getInputStream(), "UTF-8");
            }
            LOG.info("Request for execution: \n " + query + "\n");

            StreamManager sm = SPARQLGenerate.getStreamManager(true);
            List<Locator> locators = new ArrayList<>();
            locators.addAll(sm.locators());
            locators.forEach((locator) -> {
                sm.remove(locator);
            });
            
            Gson gson = new Gson();
            List<Object> documents = gson.fromJson(documentset, List.class);
            if (documents != null) {
                for (int i = 0; i < documents.size(); i++) {
                    StringMap document = (StringMap) documents.get(i);
                    String uri = (String) document.get("uri");
                    String doc = (String) document.get("document");
                    sm.addLocator(new UniqueLocator(uri, doc));
                    LOG.info("with document: " + uri + " = " + doc);
                }
            }
            
            locators.forEach((locator) -> {
                sm.addLocator(locator);
            });
            
            // parse the SPARQL-Generate query and create plan
            RootPlan plan = PlanFactory.create(query);

            Model model = plan.exec();

            StringWriter sw = new StringWriter();
            model.write(sw, lang, "http://example.org/");
            String ttl = sw.toString();
            LOG.log(Level.INFO, "Generated: \n" + ttl);
            return Response.ok(ttl, "text/turtle")
                    .header("Content-Disposition", "filename= message." + ext + ";")
                    .build();
        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            LOG.log(Level.INFO, "Triggered error: ", e);
            return Response.serverError().entity(sw.toString()).build();
        }
    }

}
