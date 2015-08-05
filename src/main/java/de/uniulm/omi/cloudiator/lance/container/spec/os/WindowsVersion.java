/*
 * Copyright (c) 2014-2015 University of Ulm
 *
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership.  Licensed under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package de.uniulm.omi.cloudiator.lance.container.spec.os;

public enum WindowsVersion {

	WIN_95("95"),
	WIN_98("98"),
	WIN_2000("2000"),
	WIN_ME("ME"),
	WIN_Vista("VISTA"),
	WIN_7("7"),
	WIN_8("8"),
	WIN_8_1("8.1"),
	;

	private final String number;
	
	WindowsVersion(String _number) {
		number = _number;
	}
	
	@Override
	public String toString() {
		return number;
	}
}