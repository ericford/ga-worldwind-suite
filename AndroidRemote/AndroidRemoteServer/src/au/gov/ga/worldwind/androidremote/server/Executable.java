/*******************************************************************************
 * Copyright 2012 Geoscience Australia
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
package au.gov.ga.worldwind.androidremote.server;

import au.gov.ga.worldwind.common.util.NativeLibraries;

/**
 * Main class when running the {@link Server} from an uber-jar. Initialises the
 * required native libraries.
 * 
 * @author Michael de Hoog (michael.dehoog@ga.gov.au)
 */
public class Executable
{
	public static void main(String[] args)
	{
		NativeLibraries.init();
		Server.main(args);
	}
}
