/*******************************************************************************
 * @contributor(s): Freerider Team (Group 4, IT2901 Fall 2012, NTNU)
 * @contributor(s): Freerider Team 2 (Group 3, IT2901 Spring 2013, NTNU)
 * @version: 2.0
 * 
 * Copyright 2013 Freerider Team 2
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
/*
 * @contributor(s): Freerider Team (Group 4, IT2901 Fall 2012, NTNU)
 * @version: 		1.0
 *
 * Copyright (C) 2012 Freerider Team.
 *
 * Licensed under the Apache License, Version 2.0.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied.
 *
 * See the License for the specific language governing permissions
 * and limitations under the License.
 *
 */
package no.ntnu.idi.freerider.protocol;

import java.util.List;

import no.ntnu.idi.freerider.model.Route;

/** A Response subclass for responses involving one or more Routes. */
public class RouteResponse extends Response {
	private final List<Route> routes;

	public RouteResponse(RequestType type,ResponseStatus status, List<Route> routes){
		super(type, status);
		this.routes = routes;
	}
	
	public RouteResponse(RequestType type,ResponseStatus status, String errormessage, List<Route> routes){
		super(type, status, errormessage);
		this.routes = routes;
	}


	public List<Route> getRoutes() {
		return routes;
	}

	@Override
	public String toString() {
		String ret = super.toString();
		ret += ", routes=" + (routes == null ? "NULL" : routes.size());
		return ret;
	}
}
