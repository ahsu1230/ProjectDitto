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

import org.xmlpull.v1.XmlPullParser;

// safe enum
public class X2StateType {
    public static X2StateType START_DOCUMENT = new X2StateType(XmlPullParser.START_DOCUMENT);
    public static X2StateType END_DOCUMENT = new X2StateType(XmlPullParser.END_DOCUMENT);
    public static X2StateType START_TAG = new X2StateType(XmlPullParser.START_TAG);
    public static X2StateType END_TAG = new X2StateType(XmlPullParser.END_TAG);
    public static X2StateType TEXT = new X2StateType(XmlPullParser.TEXT);
    public static X2StateType CDSECT = new X2StateType(XmlPullParser.CDSECT);
    public static X2StateType ENTITY_REF = new X2StateType(XmlPullParser.ENTITY_REF);
    public static X2StateType IGNORABLE_WHITESPACE = new X2StateType(XmlPullParser.IGNORABLE_WHITESPACE);
    public static X2StateType PROCESSING_INSTRUCTION = new X2StateType(XmlPullParser.PROCESSING_INSTRUCTION);
    public static X2StateType COMMENT = new X2StateType(XmlPullParser.COMMENT);
    public static X2StateType DOCDECL = new X2StateType(XmlPullParser.DOCDECL);
    //public static X2StateType EXCEPTION = new X2StateType("XNI EXCEPTION");

    protected String name;

    protected X2StateType(int xmlPullEvenType) {
        this(XmlPullParser.TYPES[ xmlPullEvenType ]);
    }

    protected X2StateType(String name) {
        this.name = name;
    }

    public String toString() { return name; };
}


