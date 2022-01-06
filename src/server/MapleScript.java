package server;

import java.util.ArrayList;
import java.util.List;

public class MapleScript {

    private boolean bSecondSpeaker, bPrev, bNext, bClose;
    private byte nMsgType, bParam, eColor;
    private short nLenMin, nLenMax;
    private int nSpeakerTypeId, nSecondSpeakerTemplateId, nSpeakerTemplateId, tWait, nDef, nMin, nMax, nMinInput, nMaxInput, nCorrect, nRemain;
    private String sScript, sTitle, sProblemText, sQuestion;
    private List<Object> objs = new ArrayList<>();

    public MapleScript(int nSpeakerTypeId, int nSpeakerTemplateId, byte nMsgType, byte bParam, byte eColor) {
        this.setnSpeakerTypeId(nSpeakerTypeId);
        this.setnSpeakerTemplateId(nSpeakerTemplateId);
        this.setbSecondSpeaker(false);
        this.setnMsgType(nMsgType);
        this.setbParam(bParam);
        this.seteColor(eColor);
    }

    public MapleScript(int nSpeakerTypeId, int nSpeakerTemplateId, int nSecondSpeakerTemplateId, byte nMsgType, byte bParam, byte eColor) {
        this.setnSpeakerTypeId(nSpeakerTypeId);
        this.setnSpeakerTemplateId(nSpeakerTemplateId);
        this.setnSecondSpeakerTemplateId(nSecondSpeakerTemplateId);
        this.setnMsgType(nMsgType);
        this.setbParam(bParam);
        this.seteColor(eColor);
    }

    public int nSpeakerTypeId() {
        return nSpeakerTypeId;
    }

    public void setnSpeakerTypeId(int nSpeakerTypeId) {
        this.nSpeakerTypeId = nSpeakerTypeId;
    }

    public int nSpeakerTemplateId() {
        return nSpeakerTemplateId;
    }

    public void setnSpeakerTemplateId(int nSpeakerTemplateId) {
        this.nSpeakerTemplateId = nSpeakerTemplateId;
    }

    public boolean bSecondSpeaker() {
        return bSecondSpeaker;
    }

    public void setbSecondSpeaker(boolean bSecondSpeaker) {
        this.bSecondSpeaker = bSecondSpeaker;
    }

    public int nSecondSpeakerTemplateId() {
        return nSecondSpeakerTemplateId;
    }

    public void setnSecondSpeakerTemplateId(int nSecondSpeakerTemplateId) {
        this.nSecondSpeakerTemplateId = nSecondSpeakerTemplateId;
    }

    public byte nMsgType() {
        return nMsgType;
    }

    public void setnMsgType(byte nMsgType) {
        this.nMsgType = nMsgType;
    }

    public byte bParam() {
        return bParam;
    }

    public void setbParam(byte bParam) {
        this.bParam = bParam;
    }

    public byte eColor() {
        return eColor;
    }

    public void seteColor(byte eColor) {
        this.eColor = eColor;
    }

    public String sScript() {
        return sScript;
    }

    public void setsScript(String sScript) {
        this.sScript = sScript;
    }

    public boolean bPrev() {
        return bPrev;
    }

    public void setbPrev(boolean bPrev) {
        this.bPrev = bPrev;
    }

    public boolean bNext() {
        return bNext;
    }

    public void setbNext(boolean bNext) {
        this.bNext = bNext;
    }

    public int tWait() {
        return tWait;
    }

    public void settWait(int tWait) {
        this.tWait = tWait;
    }

    public List<Object> getObjs() {
        return objs;
    }

    public void setObjs(List<Object> objs) {
        this.objs = objs;
    }

    public short nLenMin() {
        return nLenMin;
    }

    public void setnLenMin(short nLenMin) {
        this.nLenMin = nLenMin;
    }

    public short nLenMax() {
        return nLenMax;
    }

    public void setnLenMax(short nLenMax) {
        this.nLenMax = nLenMax;
    }

    public int nDef() {
        return nDef;
    }

    public void setnDef(int nDef) {
        this.nDef = nDef;
    }

    public int nMin() {
        return nMin;
    }

    public void setnMin(int nMin) {
        this.nMin = nMin;
    }

    public int nMax() {
        return nMax;
    }

    public void setnMax(int nMax) {
        this.nMax = nMax;
    }

    public boolean bClose() {
        return bClose;
    }

    public void setbClose(boolean bClose) {
        this.bClose = bClose;
    }

    public String sTitle() {
        return sTitle;
    }

    public void setsTitle(String sTitle) {
        this.sTitle = sTitle;
    }

    public String sProblemText() {
        return sProblemText;
    }

    public void setsProblemText(String sProblemText) {
        this.sProblemText = sProblemText;
    }

    public int nMinInput() {
        return nMinInput;
    }

    public void setnMinInput(int nMinInput) {
        this.nMinInput = nMinInput;
    }

    public int nMaxInput() {
        return nMaxInput;
    }

    public void setnMaxInput(int nMaxInput) {
        this.nMaxInput = nMaxInput;
    }

    public int nCorrect() {
        return nCorrect;
    }

    public void setnCorrect(int nCorrect) {
        this.nCorrect = nCorrect;
    }

    public int nRemain() {
        return nRemain;
    }

    public void setnRemain(int nRemain) {
        this.nRemain = nRemain;
    }

    public String sQuestion() {
        return sQuestion;
    }

    public void setsQuestion(String sQuestion) {
        this.sQuestion = sQuestion;
    }

}
