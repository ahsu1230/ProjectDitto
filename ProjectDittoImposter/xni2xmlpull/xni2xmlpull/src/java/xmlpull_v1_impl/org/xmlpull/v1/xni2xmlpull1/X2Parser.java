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

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import org.apache.xerces.xni.XMLString;

/**
 * This is bridge class that implements <a href="http://www.xmlpull.org/">XmlPull API</a>
 * by using X2Iterator that interats with Xerces2 XNI to pull XML events.
 *
 * @author <a href="http://www.extreme.indiana.edu/~aslom/">Aleksander Slominski</a>
 */
public class X2Parser implements XmlPullParser {
    private final static boolean DEBUG = false;
    private static final boolean TRACE_SIZING = false;

    protected final static String XML_URI = "http://www.w3.org/XML/1998/namespace";
    protected final static String XMLNS_URI = "http://www.w3.org/2000/xmlns/";
    //protected static final String FEATURE_XML_ROUNDTRIP=
    //    "http://xmlpull.org/v1/doc/features.html#xml-roundtrip";
    protected static final String FEATURE_NAMES_INTERNED =
        "http://xmlpull.org/v1/doc/features.html#names-interned";

    protected final static String PROPERTY_XMLDECL_VERSION =
        "http://xmlpull.org/v1/doc/properties.html#xmldecl-version";
    protected final static String PROPERTY_XMLDECL_STANDALONE =
        "http://xmlpull.org/v1/doc/properties.html#xmldecl-standalone";



    // global parser state -- non resettable
    protected X2Iterator x2 = new X2Iterator(this);
    protected boolean processNamespaces;

    protected String namespacePrefix[] = new String[8];
    //protected int namespacePrefixHash[];
    protected String namespaceUri[] = new String[8];
    protected int elNamespaceCount[] = new int[1]; // pointer to end of namespace stack for depth

    // global parser state -- resetable
    protected boolean seenRoot;
    protected int eventType;
    protected int lineNumber;
    protected int columnNumber;
    protected int depth;
    protected int namespaceEnd; // point to store namespace stack top
    // temporary state
    protected String text;

    void reset() {
        seenRoot = false;
        eventType = START_DOCUMENT;
        lineNumber = columnNumber = -1;
        depth = 0;
        namespaceEnd = 0;
        elNamespaceCount[0] = 0;
    }

    public X2Parser() throws XmlPullParserException {
    }

    //    /**
    //     * Set support of namespaces. Disabled by default.
    //     */
    //    public void setNamespaceAware(boolean awareness) throws XmlPullParserException
    //    {
    //        if(elStackDepth > 0 || seenRootElement) {
    //            throw new XmlPullParserException(
    //                "namespace support can only be set when not parsing");
    //        }
    //        try {
    //            pullParserConfiguration.setFeature(NAMESPACES_FEATURE_ID, awareness);
    //        }
    //        catch (Exception e) {
    //            throw new XmlPullParserException(
    //                "parser does not support feature ("+NAMESPACES_FEATURE_ID+")", e);
    //        }
    //        supportNs = awareness;
    //    }

    public void setFeature(String name,
                           boolean state) throws XmlPullParserException
    {
        //throw new XmlPullParserException("unrecognized");
        if(name == null) throw new IllegalArgumentException("feature name should not be nulll");
        if(FEATURE_PROCESS_NAMESPACES.equals(name)) {
            if(eventType != START_DOCUMENT) throw new XmlPullParserException(
                    "namespace processing feature can only be changed before parsing", this, null);
            x2.setFeature(X2Iterator.NAMESPACES_FEATURE_ID, state);
            processNamespaces = state;
        } else if(FEATURE_REPORT_NAMESPACE_ATTRIBUTES.equals(name)) {
            if(eventType != START_DOCUMENT) throw new XmlPullParserException(
                    "namespace reporting feature can only be changed before parsing", this, null);
            //reportNsAttribs = state;
            //hrow new XmlPullParserException("unsupported feature "+name);
            x2.setFeature(X2Iterator.NAMESPACE_PREFIXES_FEATURE_ID, state);
        } else if(FEATURE_NAMES_INTERNED.equals(name)) {
            //TODO check
            x2.setFeature(X2Iterator.STRING_INTERNING_FEATURE_ID, state);
        } else if(FEATURE_PROCESS_DOCDECL.equals(name)) {
            if(state == false) {
                throw new XmlPullParserException(
                    "processing DOCDECL can not be turned off");
            }
            // this feature for xerces can only be true ...
        } else if(FEATURE_VALIDATION.equals(name)) {
            if(eventType != START_DOCUMENT) throw new XmlPullParserException(
                    "validation feature can only be changed before parsing", this, null);
            x2.setFeature(X2Iterator.VALIDATION_FEATURE_ID, state);
        } else {
            throw new XmlPullParserException("unknown feature "+name);
        }

    }

    public boolean getFeature(String name)
    {
        if(name == null) throw new IllegalArgumentException("feature name should not be nulll");
        try {
            if(FEATURE_PROCESS_NAMESPACES.equals(name)) {
                return processNamespaces;
            } else if(FEATURE_REPORT_NAMESPACE_ATTRIBUTES.equals(name)) {
                //            return reportNsAttribs;
                //return reportNsAttribs;
                return x2.getFeature(X2Iterator.NAMESPACE_PREFIXES_FEATURE_ID);
            } else if(FEATURE_NAMES_INTERNED.equals(name)) {
                //return false;
                return x2.getFeature(X2Iterator.STRING_INTERNING_FEATURE_ID);
            } else if(FEATURE_PROCESS_DOCDECL.equals(name)) {
                return true; // can only be true ...
            } else if(FEATURE_VALIDATION.equals(name)) {
                return x2.getFeature(X2Iterator.VALIDATION_FEATURE_ID);
            }
        } catch(XmlPullParserException ex) {
        }
        return false;
    }

    public void setProperty(String name,
                            Object value)
        throws XmlPullParserException
    {
        throw new XmlPullParserException("unrecognized");
    }

    public Object getProperty(String name)
    {
        if(name == null) throw new IllegalArgumentException("property name should not be nulll");
        if(PROPERTY_XMLDECL_VERSION.equals(name)) {
            return x2.getVersion();
        } else if(PROPERTY_XMLDECL_STANDALONE.equals(name)) {
            String s = x2.getStandalone();
            if(s == null) {
                return null;
            } else if("yes".equals(s)) {
                return Boolean.TRUE;
            } else if("no".equals(s)) {
                return Boolean.FALSE;
            }
            // fall through ... -- should never happen
            throw new IllegalStateException(
                "XMLDecl standalone can only have value 'yes' or 'no' and not '"+s+"'"
                    +getPositionDescription());
        }
        return null;
    }

    public void setInput(Reader in) throws XmlPullParserException
    {
        reset();
        x2.setInput(in);
    }

    public void setInput(InputStream inputStream, String inputEncoding)
        throws XmlPullParserException
    {
        reset();
        x2.setInput(inputStream, inputEncoding);
    }

    public String getInputEncoding() { return x2.getEncoding(); }

    public void defineEntityReplacementText( String entityName,
                                            String replacementText )
        throws XmlPullParserException
    {
        throw new XmlPullParserException("not allowed: DTD processing is enabled");
    }

    public int getNamespaceCount(int depth) throws XmlPullParserException
    {
        if(processNamespaces == false || depth == 0) {
            return 0;
        }
        if(depth < 0 || depth > this.depth) throw new IllegalArgumentException(
                "napespace count mayt be for depth 0.."+this.depth+" not "+depth);
        return elNamespaceCount[ depth ];
    }

    public String getNamespacePrefix(int pos) throws XmlPullParserException
    {
        //throw new XmlPullParserException("not implemented");
        if(pos < namespaceEnd) {
            return namespacePrefix[ pos ];
        } else {
            throw new XmlPullParserException(
                "position "+pos+" exceeded number of available namespaces "+namespaceEnd);
        }
    }

    public String getNamespaceUri(int pos) throws XmlPullParserException
    {
        //throw new XmlPullParserException("not implemented");
        if(pos < namespaceEnd) {
            return namespaceUri[ pos ];
        } else {
            throw new XmlPullParserException(
                "position "+pos+" exceedded number of available namespaces "+namespaceEnd);
        }
    }

    public String getNamespace (String prefix)
    {
        if(prefix != null) {
            for( int i = namespaceEnd -1; i >= 0; i--) {
                if( prefix.equals( namespacePrefix[ i ] ) ) {
                    return namespaceUri[ i ];
                }
            }
            if("xml".equals( prefix )) {
                return XML_URI;
            } else if("xmlns".equals( prefix )) {
                return XMLNS_URI;
            }
        } else {
            for( int i = namespaceEnd -1; i >= 0; i--) {
                if( namespacePrefix[ i ]  == null ) {
                    return namespaceUri[ i ];
                }
            }

        }
        return null;
    }

    public int getDepth()
    {
        return depth;
    }

    public String getPositionDescription ()
    {
        return " "+TYPES[ eventType ] +
            //(fragment != null ? " seen "+printable(fragment)+"..." : "")+
            " @"+getLineNumber()+":"+getColumnNumber();
    }


    public int getLineNumber()
    {
        //return x2.getLineNumber();
        X2State state = x2.getCurrentState();
        return state != null ? state.getLineNumber() : -1;

    }

    public int getColumnNumber()
    {
        //return x2.getColumnNumber();
        X2State state = x2.getCurrentState();
        return state != null ? state.getColumnNumber() : -1;
    }


    protected boolean isS(char ch) {
        return (ch == ' ' || ch == '\n' || ch == '\r' || ch == '\t');
        // TODO || (supportXml11 && (ch == '\u0085' || ch == '\u2028');
    }

    public boolean isWhitespace() throws XmlPullParserException
    {
        if(eventType == TEXT || eventType == CDSECT) {
            X2State state = x2.getCurrentState();
            char[] buf = state.getBuf();
            int off = state.getOff();
            int len = state.getLen();
            for (int i = off; i <off+len; i++)
            {
                if(!isS(buf[ i ])) return false;
            }
            return true;
        } else if(eventType == IGNORABLE_WHITESPACE) {
            return true;
        }
        throw new XmlPullParserException("no content available to check for whitespaces");
    }

    public String getText()
    {
        if(eventType == START_DOCUMENT
           || eventType == END_DOCUMENT
           || eventType == DOCDECL
          ) {
            //throw new XmlPullParserException("no content available to read");
            //      if(roundtripSupported) {
            //          text = new String(buf, posStart, posEnd - posStart);
            //      } else {
            return null;
            //      }
        } //else if(eventType == ENTITY_REF) {
        //    return text;
        //}
        if(text == null) {
            X2State state = x2.getCurrentState();
            char[] buf = state.getBuf();
            int off = state.getOff();
            int len = state.getLen();
            text = new String(buf, off, len);
        }
        return text;

    }


    public char[] getTextCharacters(int [] holderForStartAndLength)
    {
        X2State state = x2.getCurrentState();
        //TODO combine else statements
        if( eventType == TEXT ) {
            //      if(usePC) {
            //          holderForStartAndLength[0] = pcStart;
            //          holderForStartAndLength[1] = pcEnd - pcStart;
            //          return pc;
            //      } else {
            //          holderForStartAndLength[0] = posStart;
            //          holderForStartAndLength[1] = posEnd - posStart;
            //          return buf;
            //
            //      }
            holderForStartAndLength[0] = state.getOff();
            holderForStartAndLength[1] = state.getLen();
            char[] buf = state.getBuf();
            return buf;
        } else if( eventType == START_TAG
                  || eventType == END_TAG
                  || eventType == CDSECT
                  || eventType == COMMENT
                  || eventType == PROCESSING_INSTRUCTION
                  || eventType == IGNORABLE_WHITESPACE)
        {
            //      holderForStartAndLength[0] = posStart;
            //      holderForStartAndLength[1] = posEnd - posStart;
            //      return buf;
            holderForStartAndLength[0] = state.getOff();
            holderForStartAndLength[1] = state.getLen();
            char[] buf = state.getBuf();
            return buf;
        } else if(eventType == ENTITY_REF) {
            String name = state.getName();
            holderForStartAndLength[0] = 0;
            holderForStartAndLength[1] = name.length();
            char[] buf = name.toCharArray();
            return buf;
        } else if(eventType == START_DOCUMENT
                  || eventType == END_DOCUMENT
                  || eventType == DOCDECL) {
            //throw new XmlPullParserException("no content available to read");
            holderForStartAndLength[0] = holderForStartAndLength[1] = -1;
            return null;
        } else {
            throw new IllegalArgumentException("unknown text eventType: "+eventType);
        }




    }

    String[] elUri = new String[0];

    public String getNamespace()
    {
        if(eventType == START_TAG || eventType == END_TAG) {
            if(processNamespaces) {
                X2State state = x2.getCurrentState();
                String n = state.getNamespace();
                if(n == null) return NO_NAMESPACE;
                return n;
            } else {
                return NO_NAMESPACE;
            }
        }
        return null;
    }

    public String getName()
    {
        if(eventType == START_TAG
           || eventType == END_TAG
           || eventType == ENTITY_REF)
        {
            X2State state = x2.getCurrentState();
            return state.getName();
            //      } else if(eventType == ENTITY_REF) {
            //          //      if(entityRefName == null) {
            //          //          entityRefName = newString(buf, posStart, posEnd - posStart);
            //          //      }
            //          //      return entityRefName;
            //          throw new IllegalStateException();
        } else {
            return null;
        }
    }

    public String getPrefix()
    {
        if(eventType == START_TAG || eventType == END_TAG) {
            X2State state = x2.getCurrentState();
            return state.getPrefix();
        }
        return null;
    }

    /**
     * Returns true if the current event is START_TAG and the tag
     * is degenerated
     * (e.g. &lt;foobar/&gt;).
     * <p><b>NOTE:</b> if the parser is not on START_TAG, an exception
     * will be thrown.
     */
    public boolean isEmptyElementTag() throws XmlPullParserException
    {
        if(eventType != START_TAG) {
            throw new XmlPullParserException(
                "parser must be on START_TAG to check for empty element", this, null);
        }
        X2State state = x2.getCurrentState();
        return state.isEmptyElement();
    }

    // --------------------------------------------------------------------------
    // START_TAG Attributes retrieval methods

    /**
     * Returns the number of attributes of the current start tag, or
     * -1 if the current event type is not START_TAG
     *
     * @see #getAttributeNamespace
     * @see #getAttributeName
     * @see #getAttributePrefix
     * @see #getAttributeValue
     */
    public int getAttributeCount()
    {
        if(eventType != START_TAG) return -1;
        X2State state = x2.getCurrentState();
        return state.getAttributesLength();
    }

    /**
     * Returns the namespace URI of the attribute
     * with the given index (starts from 0).
     * Returns an empty string ("") if namespaces are not enabled
     * or the attribute has no namespace.
     * Throws an IndexOutOfBoundsException if the index is out of range
     * or the current event type is not START_TAG.
     *
     * <p><strong>NOTE:</strong> if FEATURE_REPORT_NAMESPACE_ATTRIBUTES is set
     * then namespace attributes (xmlns:ns='...') must be reported
     * with namespace
     * <a href="http://www.w3.org/2000/xmlns/">http://www.w3.org/2000/xmlns/</a>
     * (visit this URL for description!).
     * The default namespace attribute (xmlns="...") will be reported with empty namespace.
     * <p><strong>NOTE:</strong>The xml prefix is bound as defined in
     * <a href="http://www.w3.org/TR/REC-xml-names/#ns-using">Namespaces in XML</a>
     * specification to "http://www.w3.org/XML/1998/namespace".
     *
     * @param zero based index of attribute
     * @return attribute namespace,
     *   empty string ("") is returned  if namesapces processing is not enabled or
     *   namespaces processing is enabled but attribute has no namespace (it has no prefix).
     */
    public String getAttributeNamespace (int index)
    {
        if(eventType != START_TAG) throw new IndexOutOfBoundsException(
                "only START_TAG can have attributes");
        if(processNamespaces == false) return NO_NAMESPACE;
        X2State state = x2.getCurrentState();
        int attributeCount = state.getAttributesLength();
        if(index < 0 || index >= attributeCount) throw new IndexOutOfBoundsException(
                "attribute position must be 0.."+(attributeCount-1)+" and not "+index);
        String n = state.getAttributeNamespace(index);
        if(n == null) return NO_NAMESPACE;
        return n;

    }


    /**
     * Returns the local name of the specified attribute
     * if namespaces are enabled or just attribute name if namespaces are disabled.
     * Throws an IndexOutOfBoundsException if the index is out of range
     * or current event type is not START_TAG.
     *
     * @param zero based index of attribute
     * @return attribute name (null is never returned)
     */
    public String getAttributeName (int index)
    {
        if(eventType != START_TAG) throw new IndexOutOfBoundsException(
                "only START_TAG can have attributes");
        X2State state = x2.getCurrentState();
        int attributeCount = state.getAttributesLength();
        if(index < 0 || index >= attributeCount) throw new IndexOutOfBoundsException(
                "attribute position must be 0.."+(attributeCount-1)+" and not "+index);
        return state.getAttributeName(index);
    }

    /**
     * Returns the prefix of the specified attribute
     * Returns null if the element has no prefix.
     * If namespaces are disabled it will always return null.
     * Throws an IndexOutOfBoundsException if the index is out of range
     * or current event type is not START_TAG.
     *
     * @param zero based index of attribute
     * @return attribute prefix or null if namespaces processing is not enabled.
     */
    public String getAttributePrefix(int index)
    {
        if(eventType != START_TAG) throw new IndexOutOfBoundsException(
                "only START_TAG can have attributes");
        if(processNamespaces == false) return null;
        X2State state = x2.getCurrentState();
        int attributeCount = state.getAttributesLength();
        if(index < 0 || index >= attributeCount) throw new IndexOutOfBoundsException(
                "attribute position must be 0.."+(attributeCount-1)+" and not "+index);
        return state.getAttributePrefix(index);
    }

    /**
     * Returns the type of the specified attribute
     * If parser is non-validating it MUST return CDATA.
     *
     * @param zero based index of attribute
     * @return attribute type (null is never returned)
     */
    public String getAttributeType(int index)
    {
        if(eventType != START_TAG) throw new IndexOutOfBoundsException(
                "only START_TAG can have attributes");
        X2State state = x2.getCurrentState();
        int attributeCount = state.getAttributesLength();
        if(index < 0 || index >= attributeCount) throw new IndexOutOfBoundsException(
                "attribute position must be 0.."+(attributeCount-1)+" and not "+index);
        return state.getAttributeType(index);
    }

    /**
     * Returns if the specified attribute was not in input was declared in XML.
     * If parser is non-validating it MUST always return false.
     * This information is part of XML infoset:
     *
     * @param zero based index of attribute
     * @return false if attribute was in input
     */
    public boolean isAttributeDefault(int index)
    {
        if(eventType != START_TAG) throw new IndexOutOfBoundsException(
                "only START_TAG can have attributes");
        X2State state = x2.getCurrentState();
        int attributeCount = state.getAttributesLength();
        if(index < 0 || index >= attributeCount) throw new IndexOutOfBoundsException(
                "attribute position must be 0.."+(attributeCount-1)+" and not "+index);
        return state.isAttributeDefault(index);
    }

    /**
     * Returns the given attributes value.
     * Throws an IndexOutOfBoundsException if the index is out of range
     * or current event type is not START_TAG.
     *
     * <p><strong>NOTE:</strong> attribute value must be normalized
     * (including entity replacement text if PROCESS_DOCDECL is false) as described in
     * <a href="http://www.w3.org/TR/REC-xml#AVNormalize">XML 1.0 section
     * 3.3.3 Attribute-Value Normalization</a>
     *
     * @see #defineEntityReplacementText
     *
     * @param zero based index of attribute
     * @return value of attribute (null is never returned)
     */
    public String getAttributeValue(int index)
    {
        if(eventType != START_TAG) throw new IndexOutOfBoundsException(
                "only START_TAG can have attributes");
        X2State state = x2.getCurrentState();
        int attributeCount = state.getAttributesLength();
        if(index < 0 || index >= attributeCount) throw new IndexOutOfBoundsException(
                "attribute position must be 0.."+(attributeCount-1)+" and not "+index);
        return state.getAttributeValue(index);
    }

    /**
     * Returns the attributes value identified by namespace URI and namespace localName.
     * If namespaces are disabled namespace must be null.
     * If current event type is not START_TAG then IndexOutOfBoundsException will be thrown.
     *
     * <p><strong>NOTE:</strong> attribute value must be normalized
     * (including entity replacement text if PROCESS_DOCDECL is false) as described in
     * <a href="http://www.w3.org/TR/REC-xml#AVNormalize">XML 1.0 section
     * 3.3.3 Attribute-Value Normalization</a>
     *
     * @see #defineEntityReplacementText
     *
     * @param namespace Namespace of the attribute if namespaces are enabled otherwise must be null
     * @param name If namespaces enabled local name of attribute otherwise just attribute name
     * @return value of attribute or null if attribute with given name does not exist
     */
    public String getAttributeValue(String namespace,
                                    String name)
    {
        if(eventType != START_TAG) throw new IndexOutOfBoundsException(
                "only START_TAG can have attributes");
        if(name == null) {
            throw new IllegalArgumentException("attribute name can not be null");
        }
        X2State state = x2.getCurrentState();
        int attributeCount = state.getAttributesLength();
        // TODO make check if namespace is interned!!! etc. for names!!!
        if(processNamespaces) {
            for(int i = 0; i < attributeCount; ++i) {
                //TODO if(namespace == attributeUri[ i ] // taking advantage of String.intern()
                //TODO check it for default namespace handling etc...
                if( ( namespace != null && namespace.equals(state.getAttributeNamespace(i)) )
                   && name.equals(state.getAttributeName(i) )
                  )
                {
                    return state.getAttributeValue(i);
                }
            }
        } else {
            if(namespace != null) throw new IllegalArgumentException(
                    "when namespaces processing is disabled attribute namespace must be null");
            for(int i = 0; i < attributeCount; ++i) {
                if(name.equals(state.getAttributeName(i)))
                {
                    return state.getAttributeValue(i);
                }
            }
        }
        return null;
    }


    // --------------------------------------------------------------------------
    // actual parsing methods

    /**
     * Returns the type of the current event (START_TAG, END_TAG, TEXT, etc.)
     *
     * @see #next()
     * @see #nextToken()
     */
    public int getEventType()
        throws XmlPullParserException
    {
        return eventType;
    }


    //-----------------------------------------------------------------------------
    // utility methods to mak XML parsing easier ...

    /**
     * Test if the current event is of the given type and if the
     * namespace and name do match. null will match any namespace
     * and any name. If the test is not passed, an exception is
     * thrown. The exception text indicates the parser position,
     * the expected event and the current event that is not meeting the
     * requirement.
     *
     * <p>Essentially it does this
     * <pre>
     *  if (type != getEventType()
     *  || (namespace != null && !namespace.equals( getNamespace () ) )
     *  || (name != null && !name.equals( getName() ) ) )
     *     throw new XmlPullParserException( "expected "+ TYPES[ type ]+getPositionDescription());
     * </pre>
     */
    public void require(int type, String namespace, String name)
        throws XmlPullParserException, IOException
    {
        if (type != getEventType()
            || (namespace != null && !namespace.equals (getNamespace()))
            || (name != null && !name.equals (getName ())) )
        {
            throw new XmlPullParserException (
                "expected event "+TYPES[ type ]
                    +(name != null ? " with name '"+name+"'" : "")
                    +(namespace != null && name != null ? " and" : "")
                    +(namespace != null ? " with namespace '"+namespace+"'" : "")
                    +" but got"
                    +(type != getEventType() ? " "+TYPES[ getEventType() ] : "")
                    +(name != null && getName() != null && !name.equals (getName ())
                          ? " name '"+getName()+"'" : "")
                    +(namespace != null && name != null
                          && getName() != null && !name.equals (getName ())
                          && getNamespace() != null && !namespace.equals (getNamespace())
                          ? " and" : "")
                    +(namespace != null && getNamespace() != null && !namespace.equals (getNamespace())
                          ? " namespace '"+getNamespace()+"'" : "")
                    +(" (postion:"+ getPositionDescription())+")");
        }

    }

    /**
     * If current event is START_TAG then if next element is TEXT then element content is returned
     * or if next event is END_TAG then empty string is returned, otherwise exception is thrown.
     * After calling this function successfully parser will be positioned on END_TAG.
     *
     * <p>The motivation for this function is to allow to parse consistently both
     * empty elements and elements that has non empty content, for example for input: <ol>
     * <li>&lt;tag>foo&lt;/tag>
     * <li>&lt;tag>&lt;/tag> (which is equivalent to &lt;tag/>) </ol>
     * both input can be parsed with the same code:
     * <pre>
     *   p.nextTag()
     *   p.requireEvent(p.START_TAG, "", "tag");
     *   String content = p.nextText();
     *   p.requireEvent(p.END_TAG, "", "tag");
     * </pre>
     * This function together with nextTag make it very easy to parse XML that has
     * no mixed content.
     *
     *
     * <p>Essentially it does this
     * <pre>
     *  if(getEventType() != START_TAG) {
     *     throw new XmlPullParserException(
     *       "parser must be on START_TAG to read next text", this, null);
     *  }
     *  int eventType = next();
     *  if(eventType == TEXT) {
     *     String result = getText();
     *     eventType = next();
     *     if(eventType != END_TAG) {
     *       throw new XmlPullParserException(
     *          "event TEXT it must be immediately followed by END_TAG", this, null);
     *      }
     *      return result;
     *  } else if(eventType == END_TAG) {
     *     return "";
     *  } else {
     *     throw new XmlPullParserException(
     *       "parser must be on START_TAG or TEXT to read text", this, null);
     *  }
     * </pre>
     */
    public String nextText() throws XmlPullParserException, IOException
    {
        if(getEventType() != START_TAG) {
            throw new XmlPullParserException(
                "parser must be on START_TAG to read next text", this, null);
        }
        int eventType = next();
        if(eventType == TEXT) {
            String result = getText();
            eventType = next();
            if(eventType != END_TAG) {
                throw new XmlPullParserException(
                    "TEXT must be immediately followed by END_TAG and not "
                        +TYPES[ getEventType() ], this, null);
            }
            return result;
        } else if(eventType == END_TAG) {
            return "";
        } else {
            throw new XmlPullParserException(
                "parser must be on START_TAG or TEXT to read text", this, null);
        }
    }

    public int nextTag() throws XmlPullParserException, IOException
    {
        next();
        if(eventType == TEXT && isWhitespace()) {  // skip whitespace
            next();
        }
        if (eventType != START_TAG && eventType != END_TAG) {
            throw new XmlPullParserException("expected START_TAG or END_TAG not "
                                                 +TYPES[ getEventType() ], this, null);
        }
        return eventType;
    }


    /**
     * This method works similarly to next() but will expose
     * additional event types (COMMENT, CDSECT, DOCDECL, ENTITY_REF, PROCESSING_INSTRUCTION, or
     * IGNORABLE_WHITESPACE) if they are available in input.
     *
     * <p>If special feature FEATURE_XML_ROUNDTRIP
     * (identified by URI: http://xmlpull.org/v1/doc/features.html#xml-roundtrip)
     * is true then it is possible to do XML document round trip ie. reproduce
     * exectly on output the XML input using getText().
     *
     * <p>Here is the list of tokens that can be  returned from nextToken()
     * and what getText() and getTextCharacters() returns:<dl>
     * <dt>START_DOCUMENT<dd>null
     * <dt>END_DOCUMENT<dd>null
     * <dt>START_TAG<dd>null
     *   unless FEATURE_XML_ROUNDTRIP enabled and then returns XML tag, ex: &lt;tag attr='val'>
     * <dt>END_TAG<dd>null
     * unless FEATURE_XML_ROUNDTRIP enabled and then returns XML tag, ex: &lt;/tag>
     * <dt>TEXT<dd>return unnormalized element content
     * <dt>IGNORABLE_WHITESPACE<dd>return unnormalized characters
     * <dt>CDSECT<dd>return unnormalized text <em>inside</em> CDATA
     *  ex. 'fo&lt;o' from &lt;!CDATA[fo&lt;o]]>
     * <dt>PROCESSING_INSTRUCTION<dd>return unnormalized PI content ex: 'pi foo' from &lt;?pi foo?>
     * <dt>COMMENT<dd>return comment content ex. 'foo bar' from &lt;!--foo bar-->
     * <dt>ENTITY_REF<dd>return unnormalized text of entity_name (&entity_name;)
     * <br><b>NOTE:</b> it is user responsibility to resolve entity reference
     * <br><b>NOTE:</b> character entities and standard entities such as
     *  &amp;amp; &amp;lt; &amp;gt; &amp;quot; &amp;apos; are reported as well
     * and are not resolved and not reported as TEXT tokens but as ENTITY_REF tokens!
     * This requirement is added to allow to do roundtrip of XML documents!
     * <dt>DOCDECL<dd>return inside part of DOCDECL ex. returns:<pre>
     * &quot; titlepage SYSTEM "http://www.foo.bar/dtds/typo.dtd"
     * [&lt;!ENTITY % active.links "INCLUDE">]&quot;</pre>
     * <p>for input document that contained:<pre>
     * &lt;!DOCTYPE titlepage SYSTEM "http://www.foo.bar/dtds/typo.dtd"
     * [&lt;!ENTITY % active.links "INCLUDE">]></pre>
     * </dd>
     * </dl>
     *
     * <p><strong>NOTE:</strong> returned text of token is not end-of-line normalized.
     *
     * @see #next
     * @see #START_TAG
     * @see #TEXT
     * @see #END_TAG
     * @see #END_DOCUMENT
     * @see #COMMENT
     * @see #DOCDECL
     * @see #PROCESSING_INSTRUCTION
     * @see #ENTITY_REF
     * @see #IGNORABLE_WHITESPACE
     */

    public int nextToken()
        throws XmlPullParserException, IOException
    {
        return nextImpl(true);
    }

    /**
     * Get next parsing event - element content wil be coalesced and only one
     * TEXT event must be returned for whole element content
     * (comments and processing instructions will be ignored and emtity references
     * must be expanded or exception mus be thrown if entity reerence can not be exapnded).
     * If element content is empty (content is "") then no TEXT event will be reported.
     *
     * <p><b>NOTE:</b> empty element (such as &lt;tag/>) will be reported
     *  with  two separate events: START_TAG, END_TAG - it must be so to preserve
     *   parsing equivalency of empty element to &lt;tag>&lt;/tag>.
     *  (see isEmptyElementTag ())
     *
     * @see #isEmptyElementTag
     * @see #START_TAG
     * @see #TEXT
     * @see #END_TAG
     * @see #END_DOCUMENT
     */

    public int next()
        throws XmlPullParserException, IOException
    {
        return nextImpl(false);
    }

    protected void ensureNamespacesCapacity(int size) {
        int namespaceSize = namespacePrefix != null ? namespacePrefix.length : 0;
        if(size >= namespaceSize) {
            int newSize = size > 7 ? 2 * size : 8; // = lucky 7 + 1 //25
            if(TRACE_SIZING) {
                System.err.println("namespaceSize "+namespaceSize+" ==> "+newSize);
            }
            String[] newNamespacePrefix = new String[newSize];
            String[] newNamespaceUri = new String[newSize];
            if(namespacePrefix != null) {
                System.arraycopy(
                    namespacePrefix, 0, newNamespacePrefix, 0, namespaceEnd);
                System.arraycopy(
                    namespaceUri, 0, newNamespaceUri, 0, namespaceEnd);
            }
            namespacePrefix = newNamespacePrefix;
            namespaceUri = newNamespaceUri;


            //      if( ! allStringsInterned ) {
            //          int[] newNamespacePrefixHash = new int[newSize];
            //          if(namespacePrefixHash != null) {
            //              System.arraycopy(
            //                  namespacePrefixHash, 0, newNamespacePrefixHash, 0, namespaceEnd);
            //          }
            //          namespacePrefixHash = newNamespacePrefixHash;
            //      }
            //prefixesSize = newSize;
            // //assert nsPrefixes.length > size && nsPrefixes.length == newSize
        }
    }





    /**
     * Make sure that we have enough space to keep element stack if passed size.
     * It will always create one additional slot then current depth
     */
    protected void ensureElementsCapacity() {
        int elStackSize = elNamespaceCount != null ? elNamespaceCount.length : 0;
        if( (depth + 1) >= elStackSize) {
            // we add at least one extra slot ...
            int newSize = (depth >= 7 ? 2 * depth : 8) + 2; // = lucky 7 + 1 //25
            if(TRACE_SIZING) {
                System.err.println("elStackSize "+elStackSize+" ==> "+newSize);
            }
            boolean needsCopying = elStackSize > 0;

            int[] iarr = new int[newSize];
            if(needsCopying) {
                System.arraycopy(elNamespaceCount, 0, iarr, 0, elStackSize);
            } else {
                // special initialization
                iarr[0] = 0;
            }
            elNamespaceCount = iarr;
        }
    }

    //    /**
    //     * simplistic implementation of hash function that has <b>constant</b>
    //     * time to compute - so it also means diminishing hash quality for long strings
    //     * but for XML parsing it should be good enough ...
    //     */
    //    protected static final int fastHash( char ch[], int off, int len ) {
    //        if(len == 0) return 0;
    //        //assert len >0
    //        int hash = ch[off]; // hash at beginnig
    //        //try {
    //        hash = (hash << 7) + ch[ off +  len - 1 ]; // hash at the end
    //        //} catch(ArrayIndexOutOfBoundsException aie) {
    //        //    aie.printStackTrace(); //should never happen ...
    //        //    throw new RuntimeException("this is violation of pre-condition");
    //        //}
    //        if(len > 16) hash = (hash << 7) + ch[ off + (len / 4)];  // 1/4 from beginning
    //        if(len > 8)  hash = (hash << 7) + ch[ off + (len / 2)];  // 1/2 of string size ...
    //        // notice that hash is at most done 3 times <<7 so shifted by 21 bits 8 bit value
    //        // so max result == 29 bits so it is quite just below 31 bits for long (2^32) ...
    //        //assert hash >= 0;
    //        return  hash;
    //    }


    // -- PRIVATE DEBUG METHODS

    private static void debug(String msg) { debug(msg, null); }

    private static void debug(String msg, Exception ex)
    {
        if(!DEBUG) {
            throw new RuntimeException(
                "only when DEBUG enabled can print messages");
        }
        System.err.println("XNI2XmlPull: "+msg+(ex != null ? " "+ex.getMessage() : "") );
        if(ex != null) ex.printStackTrace();
    }



    private boolean isContentIgnorable( X2StateType stateType) {
        return stateType == X2StateType.IGNORABLE_WHITESPACE
            || stateType == X2StateType.PROCESSING_INSTRUCTION
            || stateType == X2StateType.COMMENT
            || stateType == X2StateType.DOCDECL;
    }

    private boolean isContentText( X2StateType stateType) {
        return stateType == X2StateType.TEXT
            || stateType == X2StateType.CDSECT
            || stateType == X2StateType.ENTITY_REF;
    }

    private boolean isContentState( X2StateType stateType) {
        return isContentText(stateType) || isContentIgnorable(stateType);
    }

    private X2State gatheredText = new X2State();

    public int nextImpl(boolean tokenize) throws IOException, XmlPullParserException {
        if(eventType == END_TAG) { //OK we are now past END_TAG
            --depth;
            if(processNamespaces) {
                //TODO restore namespace top mark
                namespaceEnd = elNamespaceCount[ depth ];
            }
        }

        while(true) {
            // move to next state
            X2State state = x2.nextState();
            X2StateType type = state.getStateType();
            //if(type == X2StateType.TEXT) {
            if(isContentState(type)) {
                text = null;
                if(tokenize) {
                    if(type == X2StateType.TEXT) {
                        return eventType = XmlPullParser.TEXT;
                    } else if(type == X2StateType.CDSECT) {
                        return eventType = XmlPullParser.CDSECT;
                    } else if(type == X2StateType.ENTITY_REF) {
                        return eventType = XmlPullParser.ENTITY_REF;
                    } else if(type == X2StateType.IGNORABLE_WHITESPACE) {
                        return eventType = XmlPullParser.IGNORABLE_WHITESPACE;
                    } else if(type == X2StateType.PROCESSING_INSTRUCTION) {
                        return eventType = XmlPullParser.PROCESSING_INSTRUCTION;
                    } else if(type == X2StateType.COMMENT) {
                        return eventType = XmlPullParser.COMMENT;
                    } else if(type == X2StateType.DOCDECL) {
                        return eventType = XmlPullParser.DOCDECL;
                    } else {
                        throw new XmlPullParserException("unexpected XNI state "+type);
                    }
                }

                if(depth == 0) { // ignore everything outside (in prolog or epilog)
                    continue;
                }
                // if not tokenizing then need to gather text content
                // NOTE: important optimization - use real state instea of gatheredText if possible
                if( isContentState( x2.peekNextState().getStateType() ) ) {
                    gatheredText.reset(X2StateType.TEXT, x2);
                    while(true) {
                        if(isContentText( state.getStateType() ) ) {
                            if(DEBUG) debug("TEXT appending '"+new String(
                                                state.getBuf(), state.getOff(), state.getLen())+"'");
                            gatheredText.appendText(state.getBuf(), state.getOff(), state.getLen());
                        }
                        if( ! isContentState( x2.peekNextState().getStateType() ) ) {
                            break;
                        }
                        state = x2.nextState();
                    }

                    x2.setCurrentState(gatheredText);
                }
                X2State curState = x2.getCurrentState();
                if( isContentText( curState.getStateType() ) &&  curState.getLen() > 0) {
                    return eventType = XmlPullParser.TEXT;
                }
            } else if(type == X2StateType.START_TAG) {
                if(depth == 0) {
                    seenRoot = true;
                }
                ++depth;
                if(processNamespaces) {
                    // first take care of spece for namespaces
                    ensureElementsCapacity();
                    int nsCount = state.getNamespacesLength();
                    ensureNamespacesCapacity(namespaceEnd + nsCount);
                    // now declare all namespaces that were reported by XNI callbacks
                    for (int i = 0; i < nsCount; i++)
                    {
                        String prefix = state.getNamespacesPrefix(i);
                        if(prefix != null && prefix.length() == 0) {
                            prefix = null;
                        }
                        namespacePrefix[ namespaceEnd ] = prefix;
                        namespaceUri[ namespaceEnd ] = state.getNamespacesUri(i);
                        ++namespaceEnd;
                    }
                    elNamespaceCount[ depth ] = namespaceEnd;
                }
                return eventType = XmlPullParser.START_TAG;

            } else if(type == X2StateType.END_TAG) {
                //        if(elStackDepth > 0) {
                //            throw new XNIException(
                //                "expected element end tag '"
                //                    +elStack[elStackDepth-1].qName+"' not end of document"
                //            );
                //        }

                return eventType = XmlPullParser.END_TAG;
            } else if(type == X2StateType.START_DOCUMENT) {
                return eventType = XmlPullParser.START_DOCUMENT;
            } else if(type == X2StateType.END_DOCUMENT) {
                return eventType = XmlPullParser.END_DOCUMENT;
                //      } else if(type == X2StateType.EXCEPTION) {
                //          Exception ex = state.getException();
                //          throw new XmlPullParserException("could not parse:"+ex, this, ex);
            } else {
                throw new XmlPullParserException("internal error: unexpected state: "+state);
            }
        }
        //return -1;
    }

}

