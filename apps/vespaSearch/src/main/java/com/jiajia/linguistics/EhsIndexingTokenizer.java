package com.jiajia.linguistics;

import com.jiajia.utils.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class EhsIndexingTokenizer {

    /**
     * check if input char c is alphabet or digit
     *
     * @param c
     * @return
     */
    public static boolean isAlphabetOrDigit(char c) {
        if (((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')) || (c >= '0' && c <= '9')) {
            return true;
        }
        return false;
    }

    /**
     * check if input char c is digit
     *
     * @param c
     * @return
     */
    public static boolean isDigit(char c) {
        if (c >= '0' && c <= '9') {
            return true;
        }
        return false;
    }

    /**
     * Ehsn Token
     */
    class EToken {
        public String word;
        public String lowerCaseWord;
        public int offset;
        public String oriWord;

        public EToken(String word, int offset) {
            this.word = word;
            lowerCaseWord=word.toLowerCase();
            this.offset = offset;
            this.oriWord = word;
        }

        public EToken(String word, int offset, String oriWord) {
            this.word = word;
            lowerCaseWord=word.toLowerCase();
            this.offset = offset;
            this.oriWord = oriWord;
        }

        /**
         * 確保 Map 當 key 的時候，需要實作 equals(), hashcode()
         * @param obj
         * @return
         */
        @Override
        public boolean equals(Object obj) {
            if(obj==null) {
                return false;
            }
            if(this==obj) {
                return true;
            }

            if(getClass() == obj.getClass()) {
                return equals((EToken) obj);
            }

            return false;
        }

        public boolean equals(EToken obj) {


            return lowerCaseWord.equals(obj.lowerCaseWord) && offset==obj.offset;
        }

        @Override
        public int hashCode() {
            return Objects.hash(lowerCaseWord, offset);
        }

        @Override
        public String toString() {
            return word + " (" + offset + ")";
        }
    }

    /**
     * 針對英數及包涵 "-" "(" ")" 的另外做處理
     * ex: ce-178 或 ce&#45;178 皆會將 ce178建立index
     * @param input
     * @param keepOnce
     * @return
     */
    private void fetchTypeLikeTokens(String input, Map<EToken, EToken> keepOnce){
        boolean inEnOrNum = false;
        int enStart = 0;
        int enEnd = 0;
        boolean onceSpot = false;
        boolean inNum = false;

        input = input.replace("&#45;","-----");
        input = input.replace("&#40;","-----");
        input = input.replace("&#41;","-----");
        input = input.replace("&amp;","-----");

        // 將其餘的符號去除，以免建進index
        input = StringUtils.unescapeHtml3(input);

        for ( int i = 0; i < input.length(); i++){
            char c = (char)input.codePointAt(i);
            if(inEnOrNum){
                if(isAlphabetOrDigit(c) || c == '-'){
                    enEnd = i;
                    if(isDigit(c)){
                        inNum = true;
                    }else{
                        inNum = false;
                        onceSpot = false;
                    }
                } else if(c == '.') { // 如果是小數點，另外做判斷
                    if(inNum && !onceSpot){ // 出現第一次小數點，繼續當作是數字
                        enEnd = i;
                        onceSpot = true;
                    }else{ // 出現兩次以上小數點，不為數字直接切開
                        inEnOrNum = false;
                        onceSpot = false;

                        EToken eToken = new EToken(input.substring(enStart, enEnd).replace("-", ""), enStart, input.substring(enStart, enEnd));
                        keepOnce.put(eToken, eToken);
                    }
                } else {
                    inEnOrNum = false;
                    onceSpot = false;
                    EToken eToken = new EToken(input.substring(enStart, enEnd + 1).replace("-", ""), enStart, input.substring(enStart, enEnd + 1));
                    keepOnce.put(eToken, eToken);
                }

            }else{
                if(isAlphabetOrDigit(c) || c == '-'){
                    inEnOrNum = true;
                    enStart = i;
                    enEnd = i;

                    if(isDigit(c)){
                        inNum = true;
                    }
                }
            }
        }

        if(inEnOrNum) {
            EToken eToken = new EToken(input.substring(enStart).replace("-", ""), enStart, input.substring(enStart));
            keepOnce.put(eToken, eToken);
        }

    }

    /**
     * 針對英數及包涵 "-" "(" ")" 的另外做處理
     * ex: ce-178 或 ce&#45;178 皆會將 ce178建立index
     * @param input
     * @param keepOnce
     * @return
     */
    private void prdfetchTypeLikeTokens(String input, Map<EToken, EToken> keepOnce){
        boolean inEn = false;
        int enStart = 0;
        int enEnd = 0;
        input = input.replace("&#45;","-----");
        input = input.replace("&#40;","-----");
        input = input.replace("&#41;","-----");
        input = input.replace("&amp;","-----");

        // 將其餘的符號去除，以免建進index
        input = StringUtils.unescapeHtml3(input);

        for ( int i = 0; i < input.length(); i++){
            char c = (char)input.codePointAt(i);
            if(inEn){
                if(isAlphabetOrDigit(c) || c == '-'){
                    enEnd = i;
                } else {
                    inEn = false;
                    EToken eToken = new EToken(input.substring(enStart, enEnd + 1).replace("-", ""), enStart, input.substring(enStart, enEnd + 1));
                    keepOnce.put(eToken, eToken);
                }

            }else{
                if(isAlphabetOrDigit(c) || c == '-'){
                    inEn = true;
                    enStart = i;
                    enEnd = i;
                }
            }
        }

        if(inEn) {
            EToken eToken = new EToken(input.substring(enStart).replace("-", ""), enStart, input.substring(enStart));
            keepOnce.put(eToken, eToken);
        }

    }

    private void getEnTermForIndexToken(String input, Map<EToken, EToken> keepOnce){
        boolean inEn = false;
        int enStart = 0;
        int enEnd = 0;

        int conStart = 0;
        int conEnd = 0;
        char status = 'C';

        for ( int i = 0; i < input.length(); i++){
            char c = (char)input.codePointAt(i);
            if(inEn){
                if(isAlphabetOrDigit(c)){
                    enEnd = i;

                    conEnd = i;
                    if (c >= '0' && c <= '9'){
                        if(status == 'E'){
                            String conString = input.substring(conStart, conEnd);
                            EToken eToken = new EToken(conString, conStart);
                            keepOnce.put(eToken, eToken);
                            status = 'N';
                            conStart = i;
                        }
                    }else{
                        if(status == 'N'){
                            String conString = input.substring(conStart, conEnd);
                            EToken eToken = new EToken(conString, conStart);
                            keepOnce.put(eToken, eToken);
                            status = 'E';
                            conStart = i;
                        }
                    }

                }else {
                    inEn = false;
                    String enString = input.substring(enStart, enEnd + 1);
                    EToken eToken = new EToken(enString, enStart);
                    keepOnce.put(eToken, eToken);

                    String conString = input.substring(conStart, conEnd + 1);
                    eToken = new EToken(conString, conStart);
                    keepOnce.put(eToken, eToken);
                    status = 'C';

                }

            }else{
                if(isAlphabetOrDigit(c)){
                    inEn = true;
                    enStart = i;
                    enEnd = i;

                    if (c >= '0' && c <= '9'){
                        status = 'N';
                    }else{
                        status = 'E';
                    }
                    conStart = i;
                    conEnd = i;

                }
            }
        }

        if(inEn) {
            String enString = input.substring(enStart, input.length());

            EToken eToken = new EToken(enString, enStart);
            keepOnce.put(eToken, eToken);

            String conString = input.substring(conStart);
            eToken = new EToken(conString, conStart);
            keepOnce.put(eToken, eToken);
        }

        // 英文詞庫的term加入keepOnce
//        String lowerCaseInput = LinguisticsCase.toLowerCase(input);
//        for(Map.Entry<String, String> entry: EnTermDict.ALL_EN_DICT_INDEX.entrySet()){
//            addEnTerm(lowerCaseInput, input, 0, entry.getValue().length(), entry.getValue(), keepOnce);
//        }

    }

    public static void main(String[] args){

        EhsIndexingTokenizer ehsIndexingTokenizer = new EhsIndexingTokenizer();
        Map<EToken, EToken> keepOnce = new HashMap<>();

        System.out.println("======fetchTypeLikeTokens test======");

        System.out.println("keyword=黑金剛1.7mm CI316063121323 一年保固 象印 10人份*多段式壓力IH微電腦電子鍋NP-ZAF18 10人份 約28x42x25.5(cm) 不鏽鋼 飯匙、飯匙架、量米杯 60Hz 1370W 6.5kg 約33x47x30.5(cm) NP-ZAF18 ");

//        ehsIndexingTokenizer.prdfetchTypeLikeTokens("黑金剛1.7mm CI316063121323 一年保固 象印 10人份*多段式壓力IH微電腦電子鍋NP-ZAF18 10人份 約28x42x25.5(cm) 不鏽鋼 飯匙、飯匙架、量米杯 60Hz 1370W 6.5kg 約33x47x30.5(cm) NP-ZAF18 ", keepOnce);
//        ehsIndexingTokenizer.prdfetchTypeLikeTokens("1..3mm", keepOnce);
        System.out.println(keepOnce.toString());

        keepOnce = new HashMap<>();
//        ehsIndexingTokenizer.fetchTypeLikeTokens("黑金剛1.7mm CI316063121323 一年保固 象印 10人份*多段式壓力IH微電腦電子鍋NP-ZAF18 10人份 約28x42x25.5(cm) 不鏽鋼 飯匙、飯匙架、量米杯 60Hz 1370W 6.5kg 約33x47x30.5(cm) NP-ZAF18 ", keepOnce);
//        ehsIndexingTokenizer.fetchTypeLikeTokens("1..3mm", keepOnce);
        System.out.println(keepOnce.toString());

        keepOnce = new HashMap<>();
        ehsIndexingTokenizer.fetchTypeLikeTokens("dr.cy", keepOnce);
//        ehsIndexingTokenizer.fetchTypeLikeTokens("1..3mm", keepOnce);
        System.out.println(keepOnce.toString());

    }
}
