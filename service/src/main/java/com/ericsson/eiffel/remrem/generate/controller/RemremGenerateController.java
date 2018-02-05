/*
    Copyright 2017 Ericsson AB.
    For a full list of individual contributors, please see the commit history.
    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0
    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
*/
package com.ericsson.eiffel.remrem.generate.controller;

import com.ericsson.eiffel.remrem.generate.constants.RemremGenerateServiceConstants;
import com.ericsson.eiffel.remrem.protocol.MsgService;
import com.ericsson.eiffel.remrem.shared.VersionService;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;
import java.util.List;
import java.util.Map;


@RestController
@RequestMapping("/*")
public class RemremGenerateController {

    @Autowired
    private List<MsgService> msgServices;
    private JsonParser parser = new JsonParser();

    /**
     * Returns event information as json element based on the message protocol, taking message type and json body as
     * input.
     * <p>
     * <p>
     * Parameters:
     * mp - The message protocol , which tells us which service to invoke.
     * msgType - The type of message that needs to be generated.
     * bodyJson - The content of the message which is used in creating the event details.
     * <p>
     * Returns:
     * The event information as a json element
     */
    @RequestMapping(value = "/{mp}", method = RequestMethod.POST)
    public ResponseEntity<?> generate(@PathVariable String mp, @RequestParam("msgType") String msgType,
                                      @RequestBody JsonObject bodyJson) {
        MsgService msgService = getMessageService(mp);
        String response;
        try {
            if (msgService != null) {
                response = msgService.generateMsg(msgType, bodyJson);
                JsonElement parsedResponse = parser.parse(response);
                if (!parsedResponse.getAsJsonObject().has(RemremGenerateServiceConstants.JSON_ERROR_MESSAGE_FIELD)) {
                    return new ResponseEntity<>(parsedResponse, HttpStatus.OK);
                } else {
                    return new ResponseEntity<>(parsedResponse, HttpStatus.BAD_REQUEST);
                }
            } else {
                return new ResponseEntity<>(parser.parse(RemremGenerateServiceConstants.NO_SERVICE_ERROR),
                                            HttpStatus.SERVICE_UNAVAILABLE);
            }
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    

    /**
     * this method is used to display the versions of generate and all loaded protocols.
     *
     * @return this method returns a json with version details.
     */
    @RequestMapping(value = "/versions", method = RequestMethod.GET)
    public JsonElement getVersions() {
        Map<String, Map<String, String>> versions = new VersionService().getMessagingVersions();
        return parser.parse(versions.toString());
    }
    
    /**
     * this method returns available Eiffel event types as listed in EiffelEventType enum.
     *
     * @return string collection with event types.
     */
    @RequestMapping(value = "/event_types/{mp}", method = RequestMethod.GET)
    public ResponseEntity<Collection<String>> getEventTypes(@PathVariable("mp") String mp) {
    	MsgService msgService = getMessageService(mp);    	
    	return new ResponseEntity<Collection<String>>(msgService.getSupportedEventTypes(), HttpStatus.OK);
    }

    /**
     * Returns an eiffel event template matching the type specified in the path.
     *
     * @return json containing eiffel event template.
     */
    @RequestMapping(value = "/template/{type}/{mp}", method = RequestMethod.GET)
    public ResponseEntity<?> getEventTypeTemplate(@PathVariable("type") String type, @PathVariable("mp") String mp,
            final RequestEntity<String> requestEntity) {
        MsgService msgService = getMessageService(mp);
        JsonElement template = msgService.getEventTemplate(type);
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        if (template != null){
            if (requestEntity.getHeaders().getAccept().contains(MediaType.TEXT_HTML)) {
                return new ResponseEntity<>(buildHtmlReturnString(gson.toJson(template)), HttpStatus.OK);
            } else {
                return new ResponseEntity<>(template, HttpStatus.OK);
            }
        }
        else{
            return new ResponseEntity<>("Requested "+type+" Template Not Available",HttpStatus.NOT_FOUND);
        }
    }

    private MsgService getMessageService(String messageProtocol) {
        for (MsgService service : msgServices) {
            if (service.getServiceName().equals(messageProtocol)) {
                return service;
            }
        }
        return null;
    }

    /**
     * To display pretty formatted json in browser
     * @param rawJson json content 
     * @return html formatted json string
     */
    private String buildHtmlReturnString(final String rawJson) {
        final String htmlHead = "<!DOCTYPE html><html><body><pre>";
        final String htmlTail = "</pre></body></html>";
        return htmlHead + rawJson + htmlTail ;
    }
}
