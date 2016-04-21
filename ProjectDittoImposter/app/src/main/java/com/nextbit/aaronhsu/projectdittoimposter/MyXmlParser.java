package com.nextbit.aaronhsu.projectdittoimposter;

import android.util.Log;
import android.util.Pair;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Stack;
import java.util.Vector;

/**
 * Created by aaronhsu on 4/19/16.
 */
public class MyXmlParser implements XmlPullParser {
    private static final String TAG = "Ditto.MyXmlParser";
    private File mFile;
    private BufferedReader mReader;

    private boolean mStarted;
    private int mLineNumber;
    private Stack<XmlTag> mTagStack;

    public MyXmlParser(String filePath) throws IOException {
        this(new File(filePath));
    }

    public MyXmlParser(File fileSrc) throws IOException {
        if (fileSrc == null) {
            throw new IOException("Null file " + fileSrc.getAbsolutePath());
        }

        if (!fileSrc.exists()) {
            throw new IOException("Invalid file " + fileSrc.getAbsolutePath());
        }

        mFile = fileSrc;
        mReader = new BufferedReader(new InputStreamReader(new FileInputStream(mFile)));
        mStarted = false;
        mLineNumber = 0;
        mTagStack = new Stack<>();
    }

    public void finish() throws IOException {
        if (mReader != null) {
            mReader.close();
            mReader = null;
            mFile = null;
        }
    }

    @Override
    public String getName() {
        return getCurrentTag().mTagName;
    }

    @Override
    public int getDepth() {
        return mTagStack.size();
    }

    @Override
    public int next() throws XmlPullParserException, IOException {
        if (!mStarted) {
            mStarted = true;
            return START_DOCUMENT;
        }

        int event = -1;
        do {
            String line = mReader.readLine();
            if (line != null) {
                line = line.trim();
            }
            mLineNumber++;
            Log.d(TAG, "Line " + mLineNumber + " read: " + line);

            if (line == null) {
                event = END_DOCUMENT;
            } else if (line.startsWith("<?xml")) {
                Log.d(TAG, "ignore line");
                continue;
            } else if (line.startsWith("<!")) {
                Log.d(TAG, "comment");
                continue;
            } else if (line.startsWith("</")) {
                event = END_TAG;
                mTagStack.pop();
            } else if (line.startsWith("<")) {
                XmlTag tag = new XmlTag(line.substring(1));
                mTagStack.push(tag);
            } else if (!line.isEmpty()) {
                // is an attribute
                String[] arr = line.split("=");
                if (arr.length != 2) {
                    Log.d(TAG, "WE FUCKED UP SPLITTING! " + line + " " + arr.toString());
                } else {
                    String key = arr[0];
                    String val;
                    if (arr[1].endsWith(">")) {
                        val = arr[1].substring(0, arr[1].length() - 1);
                        event = START_TAG;
                    } else {
                        val = arr[1];
                    }
                    getCurrentTag().addAttribute(key, val);
                }
            }
        } while (event < 0);

        if (event == END_DOCUMENT) {
            finish();
        }

        if (event == START_TAG) {
            printCurrentAttributes();
        }

        return event;
    }

    @Override
    public String getPositionDescription() {
        return "Binary Xml File line # " + getLineNumber();
    }

    @Override
    public int getLineNumber() {
        return mLineNumber;
    }

    public void printCurrentAttributes() {
        if (mTagStack.isEmpty()) {
            return;
        }
        XmlTag currentTag = getCurrentTag();
        Log.d(TAG, "Current attributes for tag: " + currentTag.getName() + " with depth: " + mTagStack.size());
        for (Pair<String, String> pair : getCurrentTag().getAttributes()) {
            Log.d(TAG, "\t" + pair.first + " = " + pair.second);
        }
    }

    @Override
    public String getAttributeName(int index) {
        return getCurrentTag().getAttributeName(index);
    }

    @Override
    public String getAttributeValue(int index) {
        return getCurrentTag().getAttributeValue(index);
    }

    @Override
    public int getAttributeCount() {
        return getCurrentTag().countAttributes();
    }

    private XmlTag getCurrentTag() {
        if (mTagStack == null || mTagStack.isEmpty()) {
            return null;
        }
        return mTagStack.peek();
    }

    //
    // Unused methods
    //

    @Override
    public String getAttributeType(int index) {
        Log.d(TAG, "*********** setAttributeType() not supported ***********");
        return null;
    }

    @Override
    public boolean isAttributeDefault(int index) {
        Log.d(TAG, "*********** isAttributeDefault() not supported ***********");
        return false;
    }

    @Override
    public String getAttributeValue(String namespace, String name) {
        Log.d(TAG, "*********** getAttributeValue(namespace, name) not supported ***********");
        return null;
    }

    @Override
    public void setFeature(String name, boolean state) throws XmlPullParserException {
        Log.d(TAG, "*********** setFeature() not supported ***********");
    }

    @Override
    public boolean getFeature(String name) {
        Log.d(TAG, "*********** getFeature() not supported ***********");
        return false;
    }

    @Override
    public void setProperty(String name, Object value) throws XmlPullParserException {
        Log.d(TAG, "*********** setProperty() not supported ***********");
    }

    @Override
    public Object getProperty(String name) {
        Log.d(TAG, "*********** getProperty() not supported ***********");
        return null;
    }

    @Override
    public int getColumnNumber() {
        Log.d(TAG, "*********** getColumnNumber() not supported ***********");
        return 0;
    }

    @Override
    public void setInput(Reader in) throws XmlPullParserException {
        Log.d(TAG, "*********** setInput() not supported ***********");
    }

    @Override
    public void setInput(InputStream inputStream, String inputEncoding)
            throws XmlPullParserException {
        Log.d(TAG, "*********** setInput() not supported ***********");
    }

    @Override
    public String getInputEncoding() {
        Log.d(TAG, "*********** getInputEncoding() not supported ***********");
        return null;
    }

    @Override
    public void defineEntityReplacementText(String entityName, String replacementText)
            throws XmlPullParserException {
        Log.d(TAG, "*********** defineEntityReplacement() not supported ***********");
    }

    @Override
    public int getNamespaceCount(int depth) throws XmlPullParserException {
        Log.d(TAG, "*********** getNamespaceCount() not supported ***********");
        return 0;
    }

    @Override
    public String getNamespacePrefix(int pos) throws XmlPullParserException {
        Log.d(TAG, "*********** getNamespacePrefix() not supported ***********");
        return null;
    }

    @Override
    public String getNamespaceUri(int pos) throws XmlPullParserException {
        Log.d(TAG, "*********** getNamespaceUri() not supported ***********");
        return null;
    }

    @Override
    public String getNamespace(String prefix) {
        Log.d(TAG, "*********** getNamespace() not supported ***********");
        return null;
    }

    @Override
    public String getNamespace() {
        Log.d(TAG, "*********** getNamespace() not supported ***********");
        return null;
    }

    @Override
    public boolean isWhitespace() throws XmlPullParserException {
        Log.d(TAG, "*********** isWhiteSpace() not supported ***********");
        return false;
    }

    @Override
    public String getText() {
        Log.d(TAG, "*********** getText() not supported ***********");
        return null;
    }

    @Override
    public char[] getTextCharacters(int[] holderForStartAndLength) {
        Log.d(TAG, "*********** getTextCharacters() not supported ***********");
        return new char[0];
    }

    @Override
    public String getPrefix() {
        Log.d(TAG, "*********** getPrefix() not supported ***********");
        return null;
    }

    @Override
    public boolean isEmptyElementTag() throws XmlPullParserException {
        Log.d(TAG, "*********** isEmptyElementTag() not supported ***********");
        return false;
    }

    @Override
    public String getAttributeNamespace(int index) {
        Log.d(TAG, "*********** getAttributeNamespace() not supported ***********");
        return null;
    }

    @Override
    public String getAttributePrefix(int index) {
        Log.d(TAG, "*********** getAttributePrefix() not supported ***********");
        return null;
    }

    @Override
    public int getEventType() throws XmlPullParserException {
        Log.d(TAG, "*********** getEventType() not supported ***********");
        return 0;
    }

    @Override
    public int nextToken() throws XmlPullParserException, IOException {
        Log.d(TAG, "*********** nextToken() not supported ***********");
        return 0;
    }

    @Override
    public void require(int type, String namespace, String name)
            throws XmlPullParserException, IOException {
        Log.d(TAG, "*********** require() not supported ***********");
    }

    @Override
    public String nextText() throws XmlPullParserException, IOException {
        Log.d(TAG, "*********** nextText() not supported ***********");
        return null;
    }

    @Override
    public int nextTag() throws XmlPullParserException, IOException {
        Log.d(TAG, "*********** nextTag() not supported ***********");
        return 0;
    }

    public class XmlTag {
        private final String mTagName;
        private Vector<Pair<String, String>> mAttributes;

        public XmlTag(String tagName) {
            mTagName = tagName;
            mAttributes = new Vector<>();
        }

        public String getName() {
            return mTagName;
        }

        public void addAttribute(String key, String val) {
            mAttributes.add(new Pair(key, val));
        }

        public int countAttributes() {
            return mAttributes.size();
        }

        public String getAttributeName(int index) {
            return mAttributes.get(index).first;
        }

        public String getAttributeValue(int index) {
            return mAttributes.get(index).second;
        }

        public Vector<Pair<String, String>> getAttributes() {
            return mAttributes;
        }
    }
}
