/**
 * Copyright (C) 2002-2006 Bas Peters
 *
 * This file is part of MARC4J
 *
 * MARC4J is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public 
 * License as published by the Free Software Foundation; either 
 * version 2.1 of the License, or (at your option) any later version.
 *
 * MARC4J is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public 
 * License along with MARC4J; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.marc4j.samples;

import org.marc4j.MarcReader;
import org.marc4j.MarcXmlReader;
import org.marc4j.marc.Record;
import org.xml.sax.InputSource;

/**
 * Reads MARC XML from an input source.
 * 
 * @author Bas Peters
 */
public class ReadFromInputSourceExample {

    public static void main(String args[]) throws Exception {

        String systemId = "http://www.loc.gov/standards/marcxml/Sandburg/sandburg.xml";
        InputSource input = new InputSource(systemId);

        MarcReader reader = new MarcXmlReader(input);
        while (reader.hasNext()) {
            Record record = reader.next();
            System.out.println(record.toString());
        }

    }

}
