/* -*- mode: Java; c-basic-offset: 4; indent-tabs-mode: nil; -*-  //------100-columns-wide------>|*/
/*
 *  Copyright 2004 Aleksander Slominski [http://www.extreme.indiana.edu/~aslom]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.xmlpull.v1.xni2xmlpull1;

import java.util.Enumeration;
import org.xmlpull.mxp1_serializer.MXSerializer;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;
import org.xmlpull.v1.XmlSerializer;

/**
 * Simple facotry to speed up creation process.
 *
 * @author <a href="http://www.extreme.indiana.edu/~aslom/">Aleksander Slominski</a>
 */
public class X2ParserFactory
    extends XmlPullParserFactory
{
    protected static boolean stringCachedParserAvailable = true;

    public XmlPullParser newPullParser() throws XmlPullParserException {
        XmlPullParser pp = new X2Parser();
        for (Enumeration e = features.keys (); e.hasMoreElements ();) {
            String key = (String) e.nextElement();
            Boolean value = (Boolean) features.get(key);
            if(value != null && value.booleanValue()) {
                pp.setFeature(key, true);
            }
        }
        return pp;

    }

    public XmlSerializer newSerializer() throws XmlPullParserException {
        return new MXSerializer();
    }
}


