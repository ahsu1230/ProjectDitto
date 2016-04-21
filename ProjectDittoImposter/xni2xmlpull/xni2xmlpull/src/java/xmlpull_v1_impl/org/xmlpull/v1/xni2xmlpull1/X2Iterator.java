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
import org.apache.xerces.parsers.StandardParserConfiguration;
import org.apache.xerces.parsers.XMLDocumentParser;
import org.apache.xerces.xni.Augmentations;
import org.apache.xerces.xni.QName;
import org.apache.xerces.xni.XMLAttributes;
import org.apache.xerces.xni.XMLLocator;
import org.apache.xerces.xni.XMLResourceIdentifier;
import org.apache.xerces.xni.XMLString;
import org.apache.xerces.xni.XNIException;
import org.apache.xerces.xni.parser.XMLConfigurationException;
import org.apache.xerces.xni.parser.XMLErrorHandler;
import org.apache.xerces.xni.parser.XMLInputSource;
import org.apache.xerces.xni.parser.XMLParseException;
import org.apache.xerces.xni.parser.XMLPullParserConfiguration;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

// TODO: more efficient text normalization (see how xercerce SAX2...)
// TODO: allow to disable mixed content
// TODO: investingate how to retrieve state location in input stream

/**
 * This is Xerces 2 driver that uses XNI pull parsing capabilities to
 * implement <a href="http://www.xmlpull.org/">XmlPull V1 API</a>.
 *
 * Advantages:<ul>
 * <li>uses Xerces 2 and bases in stable and standard compliant parser
 * <li>uses Xerces 2 XNI in pull parser mode but hides complexity
 *    of working with XNI with simple XmlPull API
 * </ul>
 *
 * Limitations: <ul>
 * <li>this is beta version - may have still bugs :-)
 * Please report them to
 * <a href="http://www.extreme.indiana.edu/bugzilla/buglist.cgi?product=XNI2XmlPull">bugzilla issue trucker</a>.
 * </ul>
 *
 * @author <a href="http://www.extreme.indiana.edu/~aslom/">Aleksander Slominski</a>
 */
public class X2Iterator extends XMLDocumentParser implements XMLErrorHandler {
    private final static boolean DEBUG = false;
    private final static boolean PRINT_ERROR = false;
    private final static boolean TRACE_SIZING = false;



    // ----- XNI


    // --- to access Xerces 2 resources

    public static final String NAMESPACES_FEATURE_ID
        = "http://xml.org/sax/features/namespaces";

    public static final String NAMESPACE_PREFIXES_FEATURE_ID
        = "http://xml.org/sax/features/namespace-prefixes";

    public static final String VALIDATION_FEATURE_ID
        = "http://xml.org/sax/features/validation";

    public static final String STRING_INTERNING_FEATURE_ID
        = "http://xml.org/sax/features/string-interning";

    private static final String XERCES_FEATURE_PREFIX = "http://apache.org/xml/features/";
    private static final String NOTIFY_BUILTIN_REFS_FEATURE_ID
        = XERCES_FEATURE_PREFIX + "scanner/notify-builtin-refs";
    private static final String NOTIFY_CHAR_REFS_FEATURE_ID
        = XERCES_FEATURE_PREFIX + "scanner/notify-char-refs";

    protected final static String XMLNS_URI = "http://www.w3.org/2000/xmlns/";



    //public static final String STRING_INTERNING_FEATURE = "string-interning";

    // -- not resettable fields

    protected XMLPullParserConfiguration pullParserConfiguration;

    // reused when parising as value container
    protected QName attrQName = new QName();

    protected boolean reportNsAttribs;
    protected boolean processNamespaces;

    // --- resettable fields

    // used to handle problem of not evaluating setFeature after setInputSource
    //    --> deferring setInputSource as  long as possible!!!
    protected boolean needToSetInput;

    protected boolean startTagInitialized;


    protected XMLInputSource inputSource;

    // only needed to get better error messages in exception
    protected XmlPullParser xmlPullParser;


    protected X2StateQueue queue = new X2StateQueue(this);
    private X2State currentState;
    private X2StateType fillInStateType;
    private String fillInName;

    protected XMLLocator locator;

    // global varialbe representing cutrrent encoding
    protected String encoding;
    protected String standalone; // from XMLDecl
    protected String version; // from XMLDecl

    protected void reset() {
        needToSetInput = true;
        startTagInitialized = false;
        inputSource = null;
        xmlPullParser = null;

        queue.clear();
        currentState = queue.append(X2StateType.START_DOCUMENT);
        fillInStateType = null;
        fillInName = null;

        locator = null;
        encoding = null;
        standalone = null;
        version = null;
    }

    /**
     * Create instance of pull parser.
     * <p><b>NOTE:</b>parser is only eded to get better error messages in exception
     * and can be safely passed null if this iterator is not used inside XmlPullParser
     */
    public X2Iterator(XmlPullParser parser) throws XmlPullParserException {
        pullParserConfiguration = new StandardParserConfiguration();
        pullParserConfiguration.setDocumentHandler(this);
        pullParserConfiguration.setErrorHandler(this);
        pullParserConfiguration.setFeature(
            "http://apache.org/xml/features/continue-after-fatal-error", true);
        pullParserConfiguration.setFeature(
            "http://apache.org/xml/features/allow-java-encodings", true);
        pullParserConfiguration.setFeature(
            NOTIFY_BUILTIN_REFS_FEATURE_ID, true);
        pullParserConfiguration.setFeature(
            NOTIFY_CHAR_REFS_FEATURE_ID, true);
        pullParserConfiguration.setFeature(NAMESPACES_FEATURE_ID, false);
        //TODO setNamespaceAware(false);
        this.xmlPullParser = parser;


    }


    public void setFeature(String name,
                           boolean state) throws XmlPullParserException
    {
        try {
            if(NAMESPACES_FEATURE_ID.equals(name)) {
                pullParserConfiguration.setFeature(name, state);
                processNamespaces = state;
            } else if(NAMESPACE_PREFIXES_FEATURE_ID.equals(name)) {
                // this is not handled by XNI but by callback startElemen(0 in this class
                reportNsAttribs = state;
            } else {
                pullParserConfiguration.setFeature(name, state);
            }
        } catch(XMLConfigurationException ex) {
            throw new XmlPullParserException(
                "could not set feature "+name+" to "+state+" : "+ex, xmlPullParser, ex);
        }
    }

    public boolean getFeature(String name) throws XmlPullParserException
    {
        try {
            if(NAMESPACES_FEATURE_ID.equals(name)) {
                return processNamespaces;
            } else if(NAMESPACE_PREFIXES_FEATURE_ID.equals(name)) {
                return reportNsAttribs;
            } else {
                return pullParserConfiguration.getFeature(name);
            }
        } catch(XMLConfigurationException ex) {
            throw new XmlPullParserException(
                "could not retrieve feature "+name+" : "+ex, xmlPullParser, ex);
        }
    }

    //
    // --- XMLErrorHandler methods

    /** Warning. */
    public void warning(String domain, String key, XMLParseException ex)
        throws XNIException
    {
        if(PRINT_ERROR || DEBUG) error("XMLErrorHandler warning()", ex);
    }

    /** Error. */
    public void error(String domain, String key, XMLParseException ex)
        throws XNIException
    {
        if(PRINT_ERROR || DEBUG) error("XMLErrorHandler error()", ex);
        throw ex;
    }

    /** Fatal error. */
    public void fatalError(String domain, String key, XMLParseException ex)
        throws XNIException
    {
        if(PRINT_ERROR || DEBUG) error("XMLErrorHandler fatalError()", ex);
        throw ex;
        //X2State ev = queue.append(X2StateType.EXCEPTION);
        //ev.setException(ex);
    }

    //
    // --- misc methods
    //
    public String getEncoding() {
        return encoding;
    }

    public String getVersion() {
        return version;
    }

    public String getStandalone() {
        return standalone;
    }
    //
    // --- XMLDocumentHandler methods
    //

    public void startDocument(XMLLocator locator, String encoding, Augmentations augs)
        throws XNIException
    {
        this.locator = locator;
        this.encoding = encoding;
        if(DEBUG) debug("startDocument locator="+locator+" encoding="+encoding);
        //seenRootElement = false;
        //startTagInitialized = false;
        //seenContent = false;
        //nonWhitespaceContent = false;
    }

    public void endDocument(Augmentations augs) throws XNIException {
        X2State ev = queue.append(X2StateType.END_DOCUMENT);
    } // endDocument()

    private void initializeStartTag() {
        if(startTagInitialized) {
            throw new IllegalStateException();
        }
        X2State ev = queue.append(X2StateType.START_TAG);
        startTagInitialized = true;
    }


    public void startPrefixMapping(String prefix, String uri, Augmentations augs)
        throws XNIException
    {
        if(!startTagInitialized) {
            initializeStartTag();
        }
        X2State state = queue.top();
        if(state.getStateType() != X2StateType.START_TAG) {
            throw new IllegalStateException();
        }


        //        if(prefix == null || "".equals(prefix)) {
        //          if(el.defaultNs != null) {
        //              throw new XNIException(
        //                  "default namespace was alredy declared by xmlns attribute");
        //          }
        //          if(DEBUG) debug("adding default uri="+uri);
        //          el.defaultNs = uri;
        //        } else {
        //          if(el.prefixesEnd >= el.prefixesSize) {
        //              el.ensureCapacity(el.prefixesEnd);
        //          }
        //          el.prefixes[el.prefixesEnd] = prefix;
        //          el.namespaceURIs[el.prefixesEnd] = uri;
        //          el.prefixPrevNs[el.prefixesEnd] =
        //              (String) prefix2Ns.get(prefix);
        //          ++el.prefixesEnd;
        //          if(DEBUG) debug("adding prefix="+prefix+" uri="+uri);
        //          prefix2Ns.put(prefix, uri);
        //        }
        state.addNamespaceDeclaration(prefix, uri);
    }


    public void startElement(QName element, XMLAttributes attributes, Augmentations augs)
        throws XNIException
    {
        if(!startTagInitialized) {
            initializeStartTag();
        }
        X2State state = queue.top();
        if(state.getStateType() != X2StateType.START_TAG) {
            throw new IllegalStateException();
        }
        startTagInitialized = false;
        state.setEmptyElement(false);

        //        el.qName = element.rawname;
        //        if(supportNs) {
        //          el.localName = element.localpart;
        //          el.prefix = element.prefix;
        //          el.uri = element.uri;
        //        } else {
        //          el.localName = el.qName;
        //          el.prefix = null;
        //          el.uri = null; //TODO is it correct for non namespaced?
        //        }
        state.setNamespace(element.uri);
        //TODO if ! namespapcesEnabled  element.rawname ...
        state.setNamespace(element.uri);
        if(element.prefix != null && element.prefix.length() == 0) {
            state.setPrefix(null);
        } else {
            state.setPrefix(element.prefix);
        }
        state.setName(element.localpart);



        // process all attributes
        int length = attributes.getLength();
        state.ensureAttributesLength(length);
        for (int i = 0; i < length; i++) {
            attributes.getName(i, attrQName);
            //TODO better how xmlns and xmlns: attribs are handled?
            //      if(supportNs) {
            //          xmlnsAttrib = ("xmlns".equals(attrQName.rawname)
            //                             || attrQName.rawname.startsWith("xmlns:"));
            //          if(xmlnsAttrib && reportNsAttribs == false) {
            //              continue; // skip NS attrib
            //          }
            //      }
            //      if(attrPosEnd >= attrPosSize) ensureAttribs(attrPosEnd + 1);
            //      X2Attribute ap = attrPos[attrPosEnd];
            //      ap.qName = attrQName.rawname;
            //      ap.xmlnsAttrib = xmlnsAttrib;
            //      ap.prefix = attrQName.prefix;
            //      ap.localName = attrQName.localpart;
            //      ap.uri = attrQName.uri;
            //      //String attrType = attributes.getType(i);
            //      ap.value = attributes.getValue(i);
            //      ++attrPosEnd;
            // name, namespace, type (CDATA), value, specified

            //                    if (fNormalizeData) {
            //                        AttributePSVI attrPSVI = (AttributePSVI)attributes.getAugmentations(i).getItem(Constants.ATTRIBUTE_PSVI);
            //                        if (attrPSVI != null &&
            //                            attrPSVI.getValidationAttempted() == AttributePSVI.FULL_VALIDATION) {
            //                            attributes.setValue(i, attrPSVI.getSchemaNormalizedValue());
            //                        }
            //                    }

            String namespace = attrQName.uri;
            if(namespace == null) {
                namespace = ""; //empty string - that is what XmlPull API wants for NO_NAMESPACE :-)
            }

            if(processNamespaces
               && ((attrQName.prefix != null && attrQName.prefix.equals("xmlns"))
                       || attrQName.rawname.equals("xmlns"))
              )
            {
                if(!reportNsAttribs) {
                    continue; // skip this attribute
                }
                if(attrQName.prefix != null && attrQName.prefix.equals("xmlns")) {
                    namespace = XMLNS_URI;
                }
            }


            String prefix = attrQName.prefix;
            String name = attrQName.localpart;
            String value = attributes.getValue(i);
            String type = attributes.getType(i);
            boolean specified = attributes.isSpecified(i); //spe ified in xml instance document
            state.addAttribute(namespace, prefix, name, value, specified, type);

        }

        //        if(el.defaultNs == null) {
        //          if(elStackDepth > 0) {
        //              el.defaultNs = elStack[elStackDepth - 1].defaultNs;
        //          } else {
        //              el.defaultNs  = "";
        //          }
        //        }

        //if(DEBUG) debug("startTag() adding element el="+el+getPosDesc());

    }

    public void emptyElement(QName element, XMLAttributes attributes, Augmentations augs)
        throws XNIException
    {
        if(DEBUG) debug("emptyElement() called for "+element);
        startElement(element, attributes, augs);
        X2State state = queue.top();
        if(state.getStateType() != X2StateType.START_TAG) {
            throw new IllegalStateException();
        }
        state.setEmptyElement(true);
        endElement(element, augs);
        if(DEBUG) debug("emptyElement() exit for "+element);
        //endElement(element);
    }

    //    private void endElement() {
    //        if(elStackDepth < 1)  { // should never happen...
    //            throw new XNIException(
    //                "end tag without start stag");
    //        }
    //        if(seenContent) {
    //            gotContent = true;
    //        }
    //        X2ElementContent el = elStack[elStackDepth-1];
    //        if(DEBUG) debug("end element el="+el);
    //        // restore declared namespaces
    //        if(supportNs && el.prefixes != null) {
    //            //NOTE: it is in REVERSE order!
    //            for(int i = el.prefixesEnd - 1; i >= 0; --i) {
    //                //System.err.println("el="+el);
    //                if( el.prefixPrevNs[i] != null) {
    //                    prefix2Ns.put( el.prefixes[i], el.prefixPrevNs[i] );
    //                } else {
    //                    prefix2Ns.remove( el.prefixes[i] );
    //                }
    //            }
    //            // el.prefixesEnd = 0;  // this would prstate readNamesapces* from working
    //        }
    //    }

    public void endElement(QName element, Augmentations augs) throws XNIException
    {
        X2State state = queue.append(X2StateType.END_TAG);
        state.setNamespace(element.uri);
        //TODO if ! namespapcesEnabled  element.rawname ...
        if(element.prefix != null && element.prefix.length() == 0) {
            state.setPrefix(null);
        } else {
            state.setPrefix(element.prefix);
        }
        state.setName(element.localpart);
    }


    public void characters(XMLString text, Augmentations augs) throws XNIException
    {
        X2StateType target = fillInStateType != null ? fillInStateType : X2StateType.TEXT;
        if(DEBUG) debug("content='"+escape(new String(text.ch, text.offset, text.length)+"'")+
                            " to "+target+" fillInName="+fillInName);
        //        seenContent = true;
        //        gotContent = false;
        //        addNormalizedContent(text);
        //        contentstateEnd = getCurrentEntityAbsoluteOffset();
        X2State state = queue.append(target);
        fillInStateType = null;
        if(fillInName != null) {
            state.setName(fillInName);
            fillInName = null;
        }
        state.setString(text);
        if(DEBUG) debug("added content '"+escape(text.toString())+"'");
    }

    public void ignorableWhitespace(XMLString text, Augmentations augs) throws XNIException
    {
        if(DEBUG) debug("ignorable whitespace='"+escape(new String(text.ch, text.offset, text.length)+"'"));
        //characters(text, augs);
        X2State ev = queue.append(X2StateType.IGNORABLE_WHITESPACE);
        ev.setString(text);
        if(DEBUG) debug("added ignorable whitespace '"+escape(text.toString())+"'");
    }

    public void endPrefixMapping(String prefix, Augmentations augs) throws XNIException
    {
        // useless as does not contain previous uri to be restored ...
    }


    public void xmlDecl(String version, String encoding, String standalone, Augmentations augs)
        throws XNIException
    {
        //X2State ev = queue.append(X2StateType.PROCESSING_INSTRUCTION);
        // text = mere target + ' ' + data
        //XMLString string
        //ev.setString();
        this.version = version;
        this.encoding = encoding; //overwrite previous encoding
        this.standalone = standalone;
        if(DEBUG) debug("processed XMLDECL version="+version
                            +" encoding="+encoding+" standalone="+standalone+" augs="+augs);
    }

    public void doctypeDecl(String rootElement, String publicId, String systemId, Augmentations augs)
        throws XNIException
    {
        X2State ev = queue.append(X2StateType.DOCDECL);
        //ev.setString(text);
        if(DEBUG) debug("added DOCDECL"); // '"+escape(text.toString())+"'");
    }

    public void comment(XMLString text, Augmentations augs) throws XNIException
    {
        X2State ev = queue.append(X2StateType.COMMENT);
        ev.setString(text);
        if(DEBUG) debug("added COMMENT '"+escape(text.toString())+"'");
    }

    public void processingInstruction(String target, XMLString data, Augmentations augs)
        throws XNIException
    {
        X2State ev = queue.append(X2StateType.PROCESSING_INSTRUCTION);
        String text = target + ' ' + new String(data.ch, data.offset, data.length);
        char[] textBuf = text.toCharArray();
        ev.setString(new XMLString(textBuf, 0, textBuf.length));
        if(DEBUG) debug("added PI '"+escape(data.toString())+"'");
    }

    public void startGeneralEntity(String name,
                                   XMLResourceIdentifier identifier,
                                   String encoding,
                                   Augmentations augs) throws XNIException
    {
        if(DEBUG) debug("startGeneralEntity(name="+name+" identifier="+identifier+
                            " encoding="+encoding);
        fillInName = name;
        fillInStateType = X2StateType.ENTITY_REF;
    }

    public void endGeneralEntity(String name, Augmentations augs)
        throws XNIException
    {
        if(DEBUG) debug("endGeneralEntity(name="+name);
        fillInName = null;
        fillInStateType = null;
    }

    public void startEntity(String name,
                            String publicId, String systemId,
                            String baseSystemId,
                            String encoding,
                            Augmentations augs) throws XNIException
    {
        if(DEBUG) debug("startEntity(name="+name+" publicId="+publicId+" systemId="+systemId+
                            "baseSystemId="+baseSystemId+" encoding="+encoding);
    }
    public void textDecl(String version, String encoding, Augmentations augs) throws XNIException
    {
        if(DEBUG) debug("textDecl(version="+version+" encoding="+encoding);

    }
    public void endEntity(String name, Augmentations augs) throws XNIException
    {
        if(DEBUG) debug("endEntity(name="+name);
    }

    public void startCDATA(Augmentations augs) throws XNIException
    {
        if(DEBUG) debug("startCDATA()");
        fillInStateType = X2StateType.CDSECT;
        fillInName = null;
    }
    public void endCDATA(Augmentations augs) throws XNIException
    {
        if(DEBUG) debug("endCDATA()");
        fillInStateType = null;
        fillInName = null;
    }


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


    private static void error(String msg, Exception ex)
    {
        if(!DEBUG && !PRINT_ERROR) {
            throw new RuntimeException(
                "only when DEBUG or REPORT_ERROR enabled can print messages");
        }
        System.err.println("XNI2XmlPull ERROR: "+msg+(ex != null ? " "+ex.getMessage() : "") );
        if(ex != null) ex.printStackTrace();
    }

    private static String escape(String s) {
        StringBuffer buf = new StringBuffer(s.length());
        for (int i = 0; i < s.length(); i++)
        {
            char c = s.charAt(i);
            if(c == '\n') {
                buf.append("\\n");
            } else if(c == '\r') {
                buf.append("\\r");
            } else if(c == '\t') {
                buf.append("\\t");
            } else if(c == '\\') {
                buf.append("\\");
            } else if(c == '"') {
                buf.append('"');
            } else if(c < 32) {
                buf.append("\\x"+Integer.toHexString(c));
            } else {
                buf.append(c);
            }
        }
        return buf.toString();
    }

    // --- finally real iterator methods

    public int getLineNumber() {
        return currentState != null ? currentState.getLineNumber() : -1;
    }

    public int getColumnNumber() {
        return currentState != null ? currentState.getColumnNumber() : -1;
    }

    /**
     * Reset parser and set new input.
     */
    public void setInput(Reader reader) throws XmlPullParserException {
        reset();
        if(reader != null) {
            inputSource = new XMLInputSource(null, "", null, reader, null);
            needToSetInput = true;
        }
    }

    public void setInput(InputStream stream, String encoding) throws XmlPullParserException {
        if(stream == null) {
            throw new IllegalArgumentException("input stream can not be null");
        }
        reset();
        this.encoding = encoding;
        inputSource = new XMLInputSource(null, "", null, stream, encoding);
        needToSetInput = true;
    }


    //    /**
    //     * Reset parser and set new input.
    //     */
    //    public void setInput(char[] buf) throws XmlPullParserException {
    //        setInput(new CharArrayReader(buf));
    //    }
    //
    //    public void setInput(char[] buf, int off, int len)
    //        throws XmlPullParserException
    //    {
    //        setInput(new CharArrayReader(buf, off, len));
    //    }



    protected void nextImpl(boolean peek) throws IOException, XmlPullParserException {

        if(inputSource == null) {
            throw new XmlPullParserException(
                "setInput must be called before can start parsing");
        }
        if(needToSetInput) {
            needToSetInput = false;
            try {
                pullParserConfiguration.setInputSource(inputSource);
            } catch(IOException ex) {
                throw new XmlPullParserException(
                    "could not set input to reader", null, ex);
            }
        }
        if(queue.empty() || currentState.getStateType() == X2StateType.START_DOCUMENT ) {
            if(!peek) {
                queue.reset();
            }
            while(queue.empty()) {
                try {
                    if(pullParserConfiguration.parse(false) == false) {
                        queue.append(X2StateType.END_DOCUMENT);
                    }
                } catch(XNIException ex) {
                    //ex.printStackTrace();
                    throw new XmlPullParserException(
                        "could not parse: "+ex, xmlPullParser, ex);
                }
            }
        }

    }
    public X2State nextState() throws IOException, XmlPullParserException {
        nextImpl(false);
        return currentState = queue.remove();
    }

    public X2State getCurrentState() {
        return currentState;
    }

    public void setCurrentState(X2State state) {
        currentState = state;
    }

    public X2State peekNextState() throws IOException, XmlPullParserException {
        nextImpl(true);
        return queue.peekBottom();
    }
}

