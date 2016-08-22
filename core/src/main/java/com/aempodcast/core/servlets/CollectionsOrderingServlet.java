/*
 *  MIT License
 *
 *  Copyright (c) 2016 AEMPodcast.com and Joey Smith
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in all
 *  copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  SOFTWARE.
 */
package com.aempodcast.core.servlets;

import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.apache.sling.commons.json.jcr.JsonItemWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.ServletException;
import java.io.IOException;
import java.io.StringWriter;
import java.io.PrintWriter;
import java.io.Writer;

@SlingServlet(paths = "/bin/dam/collection/order", methods = {"GET", "POST"})
public class CollectionsOrderingServlet extends SlingAllMethodsServlet {

    private ResourceResolver resourceResolver = null;
    private static final Logger log = LoggerFactory.getLogger(CollectionsOrderingServlet.class);

    @Override
    protected void doGet(final SlingHttpServletRequest request, final SlingHttpServletResponse response) throws ServletException, IOException {
    	PrintWriter writer = response.getWriter();
        RequestParameter collectionLoc = request.getRequestParameterMap().getValue("collectionPath");
        if(collectionLoc != null) {
            String collectionPath = collectionLoc.getString();
            resourceResolver = (ResourceResolver) request.getResourceResolver();
            if (resourceResolver != null) {
                Resource collection = resourceResolver.getResource(collectionPath);
                JSONObject collectionJson = this.collectionToJson(resourceResolver, collection.adaptTo(Node.class));
                JSONObject responseObject = new JSONObject();
                try {
					responseObject.put("current", collectionJson);
					WriteCollectionState(response, responseObject);
				} catch (JSONException e) {
					log.error(e.getMessage());
				}
            }
        } else {
            // We have no collectionPath to process - prompt for one
            writer.write("Pass a collectionPath request parameter to see the current order for any collection.");
        }
    }

    @Override
    protected void doPost(final SlingHttpServletRequest request, final SlingHttpServletResponse response) throws ServletException, IOException {

        RequestParameter collectionLoc = request.getRequestParameterMap().getValue("collectionPath");
        RequestParameter assetPaths    = request.getRequestParameterMap().getValue("paths");

        if(collectionLoc != null && assetPaths != null) {
            String collectionPath = collectionLoc.getString();
            String orderString = assetPaths.getString();
            log.debug("collectionPath: {}, orderString:[{}]", collectionPath, orderString);
            String[] orderedItems = orderString.replaceAll("\\[|\\]|\"", "").split(",");

            resourceResolver = (ResourceResolver) request.getResourceResolver();
            if (resourceResolver != null) {
                Session session = (Session) resourceResolver.adaptTo(Session.class);
                Resource collection = resourceResolver.getResource(collectionPath);

                if(collection != null) {
                    Node collectionNode = collection.adaptTo(Node.class);
                    try {
                        if (collectionNode.hasNode("sling:members")) {
                            Node membersNode = collectionNode.getNode("sling:members");
                            if(membersNode != null && membersNode.hasProperty("sling:resources")) {
                            	JSONObject beforeOrdering =  this.collectionToJson(resourceResolver, collectionNode);
                                membersNode.setProperty("sling:resources", orderedItems);
                                session.save();
                                JSONObject afterOrdering = this.collectionToJson(resourceResolver, collectionNode);
                                JSONObject bothStates = new JSONObject();
                                try {
									bothStates.put("before", beforeOrdering);
									bothStates.put("after", afterOrdering);
								} catch (JSONException e) {
									log.error("Unable to prepare JSON response: {}", e.getMessage());
								}
                                WriteCollectionState(response, bothStates);
                            }
                        }
                    } catch(RepositoryException re) {
                        log.error(re.getMessage());
                    }
                }
                session.logout();
            }
        }
    }
    
    private void WriteCollectionState(final SlingHttpServletResponse response, final JSONObject collectionState) throws IOException {
    	PrintWriter writer = response.getWriter();
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json");
        response.setStatus(SlingHttpServletResponse.SC_OK);
        writer.write(collectionState.toString());
    }

    private JSONObject collectionToJson(final ResourceResolver resolver, final Node node) {
        /* Based on Nate Yolles' code:
         * http://www.nateyolles.com/blog/2015/12/converting-aem-sling-resources-to-json
         */
        final StringWriter stringWriter = new StringWriter();
        final JsonItemWriter jsonWriter = new JsonItemWriter(null);

        JSONObject jsonObject = null;

        try {
        	Node members = node.getNode("sling:members");
        	if (members.hasProperty("sling:resources")) {
        		jsonWriter.dump(members.getProperty("sling:resources"), stringWriter);
        	} else {
        		jsonWriter.dump(node, stringWriter, -1);
        	}
            jsonObject = new JSONObject(stringWriter.toString());
        } catch (RepositoryException | JSONException jex) {
            log.error(jex.getMessage());
        }
        return jsonObject;
    }
}
