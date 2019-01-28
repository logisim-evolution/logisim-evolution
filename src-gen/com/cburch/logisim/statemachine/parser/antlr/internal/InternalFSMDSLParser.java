package com.cburch.logisim.statemachine.parser.antlr.internal;

import org.eclipse.xtext.*;
import org.eclipse.xtext.parser.*;
import org.eclipse.xtext.parser.impl.*;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.parser.antlr.AbstractInternalAntlrParser;
import org.eclipse.xtext.parser.antlr.XtextTokenStream;
import org.eclipse.xtext.parser.antlr.XtextTokenStream.HiddenTokens;
import org.eclipse.xtext.parser.antlr.AntlrDatatypeRuleToken;
import com.cburch.logisim.statemachine.services.FSMDSLGrammarAccess;



import org.antlr.runtime.*;
import java.util.Stack;
import java.util.List;
import java.util.ArrayList;

@SuppressWarnings("all")
public class InternalFSMDSLParser extends AbstractInternalAntlrParser {
    public static final String[] tokenNames = new String[] {
        "<invalid>", "<EOR>", "<DOWN>", "<UP>", "RULE_ID", "RULE_INT", "RULE_BIN", "RULE_HEX", "RULE_STRING", "RULE_ML_COMMENT", "RULE_SL_COMMENT", "RULE_WS", "RULE_ANY_OTHER", "'['", "','", "']'", "';'", "'INORDER'", "'='", "'OUTORDER'", "'+'", "'*'", "'('", "')'", "'!'", "'fsm'", "'state_machine'", "'{'", "'in'", "'out'", "'codeWidth'", "'reset'", "'}'", "'commands'", "'@'", "'state'", "'transitions'", "':'", "'set'", "'goto'", "'when'", "'->'", "'#'", "'default'", "'.'", "'=='", "'/='", "'/'", "'define'"
    };
    public static final int RULE_HEX=7;
    public static final int T__19=19;
    public static final int T__15=15;
    public static final int T__16=16;
    public static final int T__17=17;
    public static final int T__18=18;
    public static final int T__13=13;
    public static final int T__14=14;
    public static final int RULE_BIN=6;
    public static final int RULE_ID=4;
    public static final int T__26=26;
    public static final int T__27=27;
    public static final int T__28=28;
    public static final int RULE_INT=5;
    public static final int T__29=29;
    public static final int T__22=22;
    public static final int RULE_ML_COMMENT=9;
    public static final int T__23=23;
    public static final int T__24=24;
    public static final int T__25=25;
    public static final int T__20=20;
    public static final int T__21=21;
    public static final int RULE_STRING=8;
    public static final int RULE_SL_COMMENT=10;
    public static final int T__37=37;
    public static final int T__38=38;
    public static final int T__39=39;
    public static final int T__33=33;
    public static final int T__34=34;
    public static final int T__35=35;
    public static final int T__36=36;
    public static final int EOF=-1;
    public static final int T__30=30;
    public static final int T__31=31;
    public static final int T__32=32;
    public static final int RULE_WS=11;
    public static final int RULE_ANY_OTHER=12;
    public static final int T__48=48;
    public static final int T__44=44;
    public static final int T__45=45;
    public static final int T__46=46;
    public static final int T__47=47;
    public static final int T__40=40;
    public static final int T__41=41;
    public static final int T__42=42;
    public static final int T__43=43;

    // delegates
    // delegators


        public InternalFSMDSLParser(TokenStream input) {
            this(input, new RecognizerSharedState());
        }
        public InternalFSMDSLParser(TokenStream input, RecognizerSharedState state) {
            super(input, state);
             
        }
        

    public String[] getTokenNames() { return InternalFSMDSLParser.tokenNames; }
    public String getGrammarFileName() { return "InternalFSMDSL.g"; }



     	private FSMDSLGrammarAccess grammarAccess;

        public InternalFSMDSLParser(TokenStream input, FSMDSLGrammarAccess grammarAccess) {
            this(input);
            this.grammarAccess = grammarAccess;
            registerRules(grammarAccess.getGrammar());
        }

        @Override
        protected String getFirstRuleName() {
        	return "TOP";
       	}

       	@Override
       	protected FSMDSLGrammarAccess getGrammarAccess() {
       		return grammarAccess;
       	}




    // $ANTLR start "entryRuleTOP"
    // InternalFSMDSL.g:64:1: entryRuleTOP returns [EObject current=null] : iv_ruleTOP= ruleTOP EOF ;
    public final EObject entryRuleTOP() throws RecognitionException {
        EObject current = null;

        EObject iv_ruleTOP = null;


        try {
            // InternalFSMDSL.g:64:44: (iv_ruleTOP= ruleTOP EOF )
            // InternalFSMDSL.g:65:2: iv_ruleTOP= ruleTOP EOF
            {
             newCompositeNode(grammarAccess.getTOPRule()); 
            pushFollow(FOLLOW_1);
            iv_ruleTOP=ruleTOP();

            state._fsp--;

             current =iv_ruleTOP; 
            match(input,EOF,FOLLOW_2); 

            }

        }

            catch (RecognitionException re) {
                recover(input,re);
                appendSkippedTokens();
            }
        finally {
        }
        return current;
    }
    // $ANTLR end "entryRuleTOP"


    // $ANTLR start "ruleTOP"
    // InternalFSMDSL.g:71:1: ruleTOP returns [EObject current=null] : (this_FSM_0= ruleFSM | this_ConstantDefList_1= ruleConstantDefList | this_CommandStmt_2= ruleCommandStmt | this_PredicateStmt_3= rulePredicateStmt | this_EQNSpec_4= ruleEQNSpec ) ;
    public final EObject ruleTOP() throws RecognitionException {
        EObject current = null;

        EObject this_FSM_0 = null;

        EObject this_ConstantDefList_1 = null;

        EObject this_CommandStmt_2 = null;

        EObject this_PredicateStmt_3 = null;

        EObject this_EQNSpec_4 = null;



        	enterRule();

        try {
            // InternalFSMDSL.g:77:2: ( (this_FSM_0= ruleFSM | this_ConstantDefList_1= ruleConstantDefList | this_CommandStmt_2= ruleCommandStmt | this_PredicateStmt_3= rulePredicateStmt | this_EQNSpec_4= ruleEQNSpec ) )
            // InternalFSMDSL.g:78:2: (this_FSM_0= ruleFSM | this_ConstantDefList_1= ruleConstantDefList | this_CommandStmt_2= ruleCommandStmt | this_PredicateStmt_3= rulePredicateStmt | this_EQNSpec_4= ruleEQNSpec )
            {
            // InternalFSMDSL.g:78:2: (this_FSM_0= ruleFSM | this_ConstantDefList_1= ruleConstantDefList | this_CommandStmt_2= ruleCommandStmt | this_PredicateStmt_3= rulePredicateStmt | this_EQNSpec_4= ruleEQNSpec )
            int alt1=5;
            alt1 = dfa1.predict(input);
            switch (alt1) {
                case 1 :
                    // InternalFSMDSL.g:79:3: this_FSM_0= ruleFSM
                    {

                    			newCompositeNode(grammarAccess.getTOPAccess().getFSMParserRuleCall_0());
                    		
                    pushFollow(FOLLOW_2);
                    this_FSM_0=ruleFSM();

                    state._fsp--;


                    			current = this_FSM_0;
                    			afterParserOrEnumRuleCall();
                    		

                    }
                    break;
                case 2 :
                    // InternalFSMDSL.g:88:3: this_ConstantDefList_1= ruleConstantDefList
                    {

                    			newCompositeNode(grammarAccess.getTOPAccess().getConstantDefListParserRuleCall_1());
                    		
                    pushFollow(FOLLOW_2);
                    this_ConstantDefList_1=ruleConstantDefList();

                    state._fsp--;


                    			current = this_ConstantDefList_1;
                    			afterParserOrEnumRuleCall();
                    		

                    }
                    break;
                case 3 :
                    // InternalFSMDSL.g:97:3: this_CommandStmt_2= ruleCommandStmt
                    {

                    			newCompositeNode(grammarAccess.getTOPAccess().getCommandStmtParserRuleCall_2());
                    		
                    pushFollow(FOLLOW_2);
                    this_CommandStmt_2=ruleCommandStmt();

                    state._fsp--;


                    			current = this_CommandStmt_2;
                    			afterParserOrEnumRuleCall();
                    		

                    }
                    break;
                case 4 :
                    // InternalFSMDSL.g:106:3: this_PredicateStmt_3= rulePredicateStmt
                    {

                    			newCompositeNode(grammarAccess.getTOPAccess().getPredicateStmtParserRuleCall_3());
                    		
                    pushFollow(FOLLOW_2);
                    this_PredicateStmt_3=rulePredicateStmt();

                    state._fsp--;


                    			current = this_PredicateStmt_3;
                    			afterParserOrEnumRuleCall();
                    		

                    }
                    break;
                case 5 :
                    // InternalFSMDSL.g:115:3: this_EQNSpec_4= ruleEQNSpec
                    {

                    			newCompositeNode(grammarAccess.getTOPAccess().getEQNSpecParserRuleCall_4());
                    		
                    pushFollow(FOLLOW_2);
                    this_EQNSpec_4=ruleEQNSpec();

                    state._fsp--;


                    			current = this_EQNSpec_4;
                    			afterParserOrEnumRuleCall();
                    		

                    }
                    break;

            }


            }


            	leaveRule();

        }

            catch (RecognitionException re) {
                recover(input,re);
                appendSkippedTokens();
            }
        finally {
        }
        return current;
    }
    // $ANTLR end "ruleTOP"


    // $ANTLR start "entryRuleCommandStmt"
    // InternalFSMDSL.g:127:1: entryRuleCommandStmt returns [EObject current=null] : iv_ruleCommandStmt= ruleCommandStmt EOF ;
    public final EObject entryRuleCommandStmt() throws RecognitionException {
        EObject current = null;

        EObject iv_ruleCommandStmt = null;


        try {
            // InternalFSMDSL.g:127:52: (iv_ruleCommandStmt= ruleCommandStmt EOF )
            // InternalFSMDSL.g:128:2: iv_ruleCommandStmt= ruleCommandStmt EOF
            {
             newCompositeNode(grammarAccess.getCommandStmtRule()); 
            pushFollow(FOLLOW_1);
            iv_ruleCommandStmt=ruleCommandStmt();

            state._fsp--;

             current =iv_ruleCommandStmt; 
            match(input,EOF,FOLLOW_2); 

            }

        }

            catch (RecognitionException re) {
                recover(input,re);
                appendSkippedTokens();
            }
        finally {
        }
        return current;
    }
    // $ANTLR end "entryRuleCommandStmt"


    // $ANTLR start "ruleCommandStmt"
    // InternalFSMDSL.g:134:1: ruleCommandStmt returns [EObject current=null] : (otherlv_0= '[' ( ( (lv_cst_1_0= ruleConstantDef ) ) (otherlv_2= ',' ( (lv_cst_3_0= ruleConstantDef ) ) )* )? otherlv_4= ']' otherlv_5= '[' ( ( (lv_in_6_0= ruleShortInputPort ) ) (otherlv_7= ',' ( (lv_in_8_0= ruleShortInputPort ) ) )* )? otherlv_9= ']' otherlv_10= '[' ( ( (lv_out_11_0= ruleShortOutputPort ) ) (otherlv_12= ',' ( (lv_out_13_0= ruleShortOutputPort ) ) )* )? otherlv_14= ']' ( ( (lv_commands_15_0= ruleCommand ) ) (otherlv_16= ';' )? ) ( ( (lv_commands_17_0= ruleCommand ) ) (otherlv_18= ';' )? )* ) ;
    public final EObject ruleCommandStmt() throws RecognitionException {
        EObject current = null;

        Token otherlv_0=null;
        Token otherlv_2=null;
        Token otherlv_4=null;
        Token otherlv_5=null;
        Token otherlv_7=null;
        Token otherlv_9=null;
        Token otherlv_10=null;
        Token otherlv_12=null;
        Token otherlv_14=null;
        Token otherlv_16=null;
        Token otherlv_18=null;
        EObject lv_cst_1_0 = null;

        EObject lv_cst_3_0 = null;

        EObject lv_in_6_0 = null;

        EObject lv_in_8_0 = null;

        EObject lv_out_11_0 = null;

        EObject lv_out_13_0 = null;

        EObject lv_commands_15_0 = null;

        EObject lv_commands_17_0 = null;



        	enterRule();

        try {
            // InternalFSMDSL.g:140:2: ( (otherlv_0= '[' ( ( (lv_cst_1_0= ruleConstantDef ) ) (otherlv_2= ',' ( (lv_cst_3_0= ruleConstantDef ) ) )* )? otherlv_4= ']' otherlv_5= '[' ( ( (lv_in_6_0= ruleShortInputPort ) ) (otherlv_7= ',' ( (lv_in_8_0= ruleShortInputPort ) ) )* )? otherlv_9= ']' otherlv_10= '[' ( ( (lv_out_11_0= ruleShortOutputPort ) ) (otherlv_12= ',' ( (lv_out_13_0= ruleShortOutputPort ) ) )* )? otherlv_14= ']' ( ( (lv_commands_15_0= ruleCommand ) ) (otherlv_16= ';' )? ) ( ( (lv_commands_17_0= ruleCommand ) ) (otherlv_18= ';' )? )* ) )
            // InternalFSMDSL.g:141:2: (otherlv_0= '[' ( ( (lv_cst_1_0= ruleConstantDef ) ) (otherlv_2= ',' ( (lv_cst_3_0= ruleConstantDef ) ) )* )? otherlv_4= ']' otherlv_5= '[' ( ( (lv_in_6_0= ruleShortInputPort ) ) (otherlv_7= ',' ( (lv_in_8_0= ruleShortInputPort ) ) )* )? otherlv_9= ']' otherlv_10= '[' ( ( (lv_out_11_0= ruleShortOutputPort ) ) (otherlv_12= ',' ( (lv_out_13_0= ruleShortOutputPort ) ) )* )? otherlv_14= ']' ( ( (lv_commands_15_0= ruleCommand ) ) (otherlv_16= ';' )? ) ( ( (lv_commands_17_0= ruleCommand ) ) (otherlv_18= ';' )? )* )
            {
            // InternalFSMDSL.g:141:2: (otherlv_0= '[' ( ( (lv_cst_1_0= ruleConstantDef ) ) (otherlv_2= ',' ( (lv_cst_3_0= ruleConstantDef ) ) )* )? otherlv_4= ']' otherlv_5= '[' ( ( (lv_in_6_0= ruleShortInputPort ) ) (otherlv_7= ',' ( (lv_in_8_0= ruleShortInputPort ) ) )* )? otherlv_9= ']' otherlv_10= '[' ( ( (lv_out_11_0= ruleShortOutputPort ) ) (otherlv_12= ',' ( (lv_out_13_0= ruleShortOutputPort ) ) )* )? otherlv_14= ']' ( ( (lv_commands_15_0= ruleCommand ) ) (otherlv_16= ';' )? ) ( ( (lv_commands_17_0= ruleCommand ) ) (otherlv_18= ';' )? )* )
            // InternalFSMDSL.g:142:3: otherlv_0= '[' ( ( (lv_cst_1_0= ruleConstantDef ) ) (otherlv_2= ',' ( (lv_cst_3_0= ruleConstantDef ) ) )* )? otherlv_4= ']' otherlv_5= '[' ( ( (lv_in_6_0= ruleShortInputPort ) ) (otherlv_7= ',' ( (lv_in_8_0= ruleShortInputPort ) ) )* )? otherlv_9= ']' otherlv_10= '[' ( ( (lv_out_11_0= ruleShortOutputPort ) ) (otherlv_12= ',' ( (lv_out_13_0= ruleShortOutputPort ) ) )* )? otherlv_14= ']' ( ( (lv_commands_15_0= ruleCommand ) ) (otherlv_16= ';' )? ) ( ( (lv_commands_17_0= ruleCommand ) ) (otherlv_18= ';' )? )*
            {
            otherlv_0=(Token)match(input,13,FOLLOW_3); 

            			newLeafNode(otherlv_0, grammarAccess.getCommandStmtAccess().getLeftSquareBracketKeyword_0());
            		
            // InternalFSMDSL.g:146:3: ( ( (lv_cst_1_0= ruleConstantDef ) ) (otherlv_2= ',' ( (lv_cst_3_0= ruleConstantDef ) ) )* )?
            int alt3=2;
            int LA3_0 = input.LA(1);

            if ( (LA3_0==48) ) {
                alt3=1;
            }
            switch (alt3) {
                case 1 :
                    // InternalFSMDSL.g:147:4: ( (lv_cst_1_0= ruleConstantDef ) ) (otherlv_2= ',' ( (lv_cst_3_0= ruleConstantDef ) ) )*
                    {
                    // InternalFSMDSL.g:147:4: ( (lv_cst_1_0= ruleConstantDef ) )
                    // InternalFSMDSL.g:148:5: (lv_cst_1_0= ruleConstantDef )
                    {
                    // InternalFSMDSL.g:148:5: (lv_cst_1_0= ruleConstantDef )
                    // InternalFSMDSL.g:149:6: lv_cst_1_0= ruleConstantDef
                    {

                    						newCompositeNode(grammarAccess.getCommandStmtAccess().getCstConstantDefParserRuleCall_1_0_0());
                    					
                    pushFollow(FOLLOW_4);
                    lv_cst_1_0=ruleConstantDef();

                    state._fsp--;


                    						if (current==null) {
                    							current = createModelElementForParent(grammarAccess.getCommandStmtRule());
                    						}
                    						add(
                    							current,
                    							"cst",
                    							lv_cst_1_0,
                    							"com.cburch.logisim.statemachine.FSMDSL.ConstantDef");
                    						afterParserOrEnumRuleCall();
                    					

                    }


                    }

                    // InternalFSMDSL.g:166:4: (otherlv_2= ',' ( (lv_cst_3_0= ruleConstantDef ) ) )*
                    loop2:
                    do {
                        int alt2=2;
                        int LA2_0 = input.LA(1);

                        if ( (LA2_0==14) ) {
                            alt2=1;
                        }


                        switch (alt2) {
                    	case 1 :
                    	    // InternalFSMDSL.g:167:5: otherlv_2= ',' ( (lv_cst_3_0= ruleConstantDef ) )
                    	    {
                    	    otherlv_2=(Token)match(input,14,FOLLOW_5); 

                    	    					newLeafNode(otherlv_2, grammarAccess.getCommandStmtAccess().getCommaKeyword_1_1_0());
                    	    				
                    	    // InternalFSMDSL.g:171:5: ( (lv_cst_3_0= ruleConstantDef ) )
                    	    // InternalFSMDSL.g:172:6: (lv_cst_3_0= ruleConstantDef )
                    	    {
                    	    // InternalFSMDSL.g:172:6: (lv_cst_3_0= ruleConstantDef )
                    	    // InternalFSMDSL.g:173:7: lv_cst_3_0= ruleConstantDef
                    	    {

                    	    							newCompositeNode(grammarAccess.getCommandStmtAccess().getCstConstantDefParserRuleCall_1_1_1_0());
                    	    						
                    	    pushFollow(FOLLOW_4);
                    	    lv_cst_3_0=ruleConstantDef();

                    	    state._fsp--;


                    	    							if (current==null) {
                    	    								current = createModelElementForParent(grammarAccess.getCommandStmtRule());
                    	    							}
                    	    							add(
                    	    								current,
                    	    								"cst",
                    	    								lv_cst_3_0,
                    	    								"com.cburch.logisim.statemachine.FSMDSL.ConstantDef");
                    	    							afterParserOrEnumRuleCall();
                    	    						

                    	    }


                    	    }


                    	    }
                    	    break;

                    	default :
                    	    break loop2;
                        }
                    } while (true);


                    }
                    break;

            }

            otherlv_4=(Token)match(input,15,FOLLOW_6); 

            			newLeafNode(otherlv_4, grammarAccess.getCommandStmtAccess().getRightSquareBracketKeyword_2());
            		
            otherlv_5=(Token)match(input,13,FOLLOW_7); 

            			newLeafNode(otherlv_5, grammarAccess.getCommandStmtAccess().getLeftSquareBracketKeyword_3());
            		
            // InternalFSMDSL.g:200:3: ( ( (lv_in_6_0= ruleShortInputPort ) ) (otherlv_7= ',' ( (lv_in_8_0= ruleShortInputPort ) ) )* )?
            int alt5=2;
            int LA5_0 = input.LA(1);

            if ( (LA5_0==RULE_ID) ) {
                alt5=1;
            }
            switch (alt5) {
                case 1 :
                    // InternalFSMDSL.g:201:4: ( (lv_in_6_0= ruleShortInputPort ) ) (otherlv_7= ',' ( (lv_in_8_0= ruleShortInputPort ) ) )*
                    {
                    // InternalFSMDSL.g:201:4: ( (lv_in_6_0= ruleShortInputPort ) )
                    // InternalFSMDSL.g:202:5: (lv_in_6_0= ruleShortInputPort )
                    {
                    // InternalFSMDSL.g:202:5: (lv_in_6_0= ruleShortInputPort )
                    // InternalFSMDSL.g:203:6: lv_in_6_0= ruleShortInputPort
                    {

                    						newCompositeNode(grammarAccess.getCommandStmtAccess().getInShortInputPortParserRuleCall_4_0_0());
                    					
                    pushFollow(FOLLOW_4);
                    lv_in_6_0=ruleShortInputPort();

                    state._fsp--;


                    						if (current==null) {
                    							current = createModelElementForParent(grammarAccess.getCommandStmtRule());
                    						}
                    						add(
                    							current,
                    							"in",
                    							lv_in_6_0,
                    							"com.cburch.logisim.statemachine.FSMDSL.ShortInputPort");
                    						afterParserOrEnumRuleCall();
                    					

                    }


                    }

                    // InternalFSMDSL.g:220:4: (otherlv_7= ',' ( (lv_in_8_0= ruleShortInputPort ) ) )*
                    loop4:
                    do {
                        int alt4=2;
                        int LA4_0 = input.LA(1);

                        if ( (LA4_0==14) ) {
                            alt4=1;
                        }


                        switch (alt4) {
                    	case 1 :
                    	    // InternalFSMDSL.g:221:5: otherlv_7= ',' ( (lv_in_8_0= ruleShortInputPort ) )
                    	    {
                    	    otherlv_7=(Token)match(input,14,FOLLOW_8); 

                    	    					newLeafNode(otherlv_7, grammarAccess.getCommandStmtAccess().getCommaKeyword_4_1_0());
                    	    				
                    	    // InternalFSMDSL.g:225:5: ( (lv_in_8_0= ruleShortInputPort ) )
                    	    // InternalFSMDSL.g:226:6: (lv_in_8_0= ruleShortInputPort )
                    	    {
                    	    // InternalFSMDSL.g:226:6: (lv_in_8_0= ruleShortInputPort )
                    	    // InternalFSMDSL.g:227:7: lv_in_8_0= ruleShortInputPort
                    	    {

                    	    							newCompositeNode(grammarAccess.getCommandStmtAccess().getInShortInputPortParserRuleCall_4_1_1_0());
                    	    						
                    	    pushFollow(FOLLOW_4);
                    	    lv_in_8_0=ruleShortInputPort();

                    	    state._fsp--;


                    	    							if (current==null) {
                    	    								current = createModelElementForParent(grammarAccess.getCommandStmtRule());
                    	    							}
                    	    							add(
                    	    								current,
                    	    								"in",
                    	    								lv_in_8_0,
                    	    								"com.cburch.logisim.statemachine.FSMDSL.ShortInputPort");
                    	    							afterParserOrEnumRuleCall();
                    	    						

                    	    }


                    	    }


                    	    }
                    	    break;

                    	default :
                    	    break loop4;
                        }
                    } while (true);


                    }
                    break;

            }

            otherlv_9=(Token)match(input,15,FOLLOW_6); 

            			newLeafNode(otherlv_9, grammarAccess.getCommandStmtAccess().getRightSquareBracketKeyword_5());
            		
            otherlv_10=(Token)match(input,13,FOLLOW_7); 

            			newLeafNode(otherlv_10, grammarAccess.getCommandStmtAccess().getLeftSquareBracketKeyword_6());
            		
            // InternalFSMDSL.g:254:3: ( ( (lv_out_11_0= ruleShortOutputPort ) ) (otherlv_12= ',' ( (lv_out_13_0= ruleShortOutputPort ) ) )* )?
            int alt7=2;
            int LA7_0 = input.LA(1);

            if ( (LA7_0==RULE_ID) ) {
                alt7=1;
            }
            switch (alt7) {
                case 1 :
                    // InternalFSMDSL.g:255:4: ( (lv_out_11_0= ruleShortOutputPort ) ) (otherlv_12= ',' ( (lv_out_13_0= ruleShortOutputPort ) ) )*
                    {
                    // InternalFSMDSL.g:255:4: ( (lv_out_11_0= ruleShortOutputPort ) )
                    // InternalFSMDSL.g:256:5: (lv_out_11_0= ruleShortOutputPort )
                    {
                    // InternalFSMDSL.g:256:5: (lv_out_11_0= ruleShortOutputPort )
                    // InternalFSMDSL.g:257:6: lv_out_11_0= ruleShortOutputPort
                    {

                    						newCompositeNode(grammarAccess.getCommandStmtAccess().getOutShortOutputPortParserRuleCall_7_0_0());
                    					
                    pushFollow(FOLLOW_4);
                    lv_out_11_0=ruleShortOutputPort();

                    state._fsp--;


                    						if (current==null) {
                    							current = createModelElementForParent(grammarAccess.getCommandStmtRule());
                    						}
                    						add(
                    							current,
                    							"out",
                    							lv_out_11_0,
                    							"com.cburch.logisim.statemachine.FSMDSL.ShortOutputPort");
                    						afterParserOrEnumRuleCall();
                    					

                    }


                    }

                    // InternalFSMDSL.g:274:4: (otherlv_12= ',' ( (lv_out_13_0= ruleShortOutputPort ) ) )*
                    loop6:
                    do {
                        int alt6=2;
                        int LA6_0 = input.LA(1);

                        if ( (LA6_0==14) ) {
                            alt6=1;
                        }


                        switch (alt6) {
                    	case 1 :
                    	    // InternalFSMDSL.g:275:5: otherlv_12= ',' ( (lv_out_13_0= ruleShortOutputPort ) )
                    	    {
                    	    otherlv_12=(Token)match(input,14,FOLLOW_8); 

                    	    					newLeafNode(otherlv_12, grammarAccess.getCommandStmtAccess().getCommaKeyword_7_1_0());
                    	    				
                    	    // InternalFSMDSL.g:279:5: ( (lv_out_13_0= ruleShortOutputPort ) )
                    	    // InternalFSMDSL.g:280:6: (lv_out_13_0= ruleShortOutputPort )
                    	    {
                    	    // InternalFSMDSL.g:280:6: (lv_out_13_0= ruleShortOutputPort )
                    	    // InternalFSMDSL.g:281:7: lv_out_13_0= ruleShortOutputPort
                    	    {

                    	    							newCompositeNode(grammarAccess.getCommandStmtAccess().getOutShortOutputPortParserRuleCall_7_1_1_0());
                    	    						
                    	    pushFollow(FOLLOW_4);
                    	    lv_out_13_0=ruleShortOutputPort();

                    	    state._fsp--;


                    	    							if (current==null) {
                    	    								current = createModelElementForParent(grammarAccess.getCommandStmtRule());
                    	    							}
                    	    							add(
                    	    								current,
                    	    								"out",
                    	    								lv_out_13_0,
                    	    								"com.cburch.logisim.statemachine.FSMDSL.ShortOutputPort");
                    	    							afterParserOrEnumRuleCall();
                    	    						

                    	    }


                    	    }


                    	    }
                    	    break;

                    	default :
                    	    break loop6;
                        }
                    } while (true);


                    }
                    break;

            }

            otherlv_14=(Token)match(input,15,FOLLOW_8); 

            			newLeafNode(otherlv_14, grammarAccess.getCommandStmtAccess().getRightSquareBracketKeyword_8());
            		
            // InternalFSMDSL.g:304:3: ( ( (lv_commands_15_0= ruleCommand ) ) (otherlv_16= ';' )? )
            // InternalFSMDSL.g:305:4: ( (lv_commands_15_0= ruleCommand ) ) (otherlv_16= ';' )?
            {
            // InternalFSMDSL.g:305:4: ( (lv_commands_15_0= ruleCommand ) )
            // InternalFSMDSL.g:306:5: (lv_commands_15_0= ruleCommand )
            {
            // InternalFSMDSL.g:306:5: (lv_commands_15_0= ruleCommand )
            // InternalFSMDSL.g:307:6: lv_commands_15_0= ruleCommand
            {

            						newCompositeNode(grammarAccess.getCommandStmtAccess().getCommandsCommandParserRuleCall_9_0_0());
            					
            pushFollow(FOLLOW_9);
            lv_commands_15_0=ruleCommand();

            state._fsp--;


            						if (current==null) {
            							current = createModelElementForParent(grammarAccess.getCommandStmtRule());
            						}
            						add(
            							current,
            							"commands",
            							lv_commands_15_0,
            							"com.cburch.logisim.statemachine.FSMDSL.Command");
            						afterParserOrEnumRuleCall();
            					

            }


            }

            // InternalFSMDSL.g:324:4: (otherlv_16= ';' )?
            int alt8=2;
            int LA8_0 = input.LA(1);

            if ( (LA8_0==16) ) {
                alt8=1;
            }
            switch (alt8) {
                case 1 :
                    // InternalFSMDSL.g:325:5: otherlv_16= ';'
                    {
                    otherlv_16=(Token)match(input,16,FOLLOW_10); 

                    					newLeafNode(otherlv_16, grammarAccess.getCommandStmtAccess().getSemicolonKeyword_9_1());
                    				

                    }
                    break;

            }


            }

            // InternalFSMDSL.g:331:3: ( ( (lv_commands_17_0= ruleCommand ) ) (otherlv_18= ';' )? )*
            loop10:
            do {
                int alt10=2;
                int LA10_0 = input.LA(1);

                if ( (LA10_0==RULE_ID) ) {
                    alt10=1;
                }


                switch (alt10) {
            	case 1 :
            	    // InternalFSMDSL.g:332:4: ( (lv_commands_17_0= ruleCommand ) ) (otherlv_18= ';' )?
            	    {
            	    // InternalFSMDSL.g:332:4: ( (lv_commands_17_0= ruleCommand ) )
            	    // InternalFSMDSL.g:333:5: (lv_commands_17_0= ruleCommand )
            	    {
            	    // InternalFSMDSL.g:333:5: (lv_commands_17_0= ruleCommand )
            	    // InternalFSMDSL.g:334:6: lv_commands_17_0= ruleCommand
            	    {

            	    						newCompositeNode(grammarAccess.getCommandStmtAccess().getCommandsCommandParserRuleCall_10_0_0());
            	    					
            	    pushFollow(FOLLOW_9);
            	    lv_commands_17_0=ruleCommand();

            	    state._fsp--;


            	    						if (current==null) {
            	    							current = createModelElementForParent(grammarAccess.getCommandStmtRule());
            	    						}
            	    						add(
            	    							current,
            	    							"commands",
            	    							lv_commands_17_0,
            	    							"com.cburch.logisim.statemachine.FSMDSL.Command");
            	    						afterParserOrEnumRuleCall();
            	    					

            	    }


            	    }

            	    // InternalFSMDSL.g:351:4: (otherlv_18= ';' )?
            	    int alt9=2;
            	    int LA9_0 = input.LA(1);

            	    if ( (LA9_0==16) ) {
            	        alt9=1;
            	    }
            	    switch (alt9) {
            	        case 1 :
            	            // InternalFSMDSL.g:352:5: otherlv_18= ';'
            	            {
            	            otherlv_18=(Token)match(input,16,FOLLOW_10); 

            	            					newLeafNode(otherlv_18, grammarAccess.getCommandStmtAccess().getSemicolonKeyword_10_1());
            	            				

            	            }
            	            break;

            	    }


            	    }
            	    break;

            	default :
            	    break loop10;
                }
            } while (true);


            }


            }


            	leaveRule();

        }

            catch (RecognitionException re) {
                recover(input,re);
                appendSkippedTokens();
            }
        finally {
        }
        return current;
    }
    // $ANTLR end "ruleCommandStmt"


    // $ANTLR start "entryRuleConstantDefList"
    // InternalFSMDSL.g:362:1: entryRuleConstantDefList returns [EObject current=null] : iv_ruleConstantDefList= ruleConstantDefList EOF ;
    public final EObject entryRuleConstantDefList() throws RecognitionException {
        EObject current = null;

        EObject iv_ruleConstantDefList = null;


        try {
            // InternalFSMDSL.g:362:56: (iv_ruleConstantDefList= ruleConstantDefList EOF )
            // InternalFSMDSL.g:363:2: iv_ruleConstantDefList= ruleConstantDefList EOF
            {
             newCompositeNode(grammarAccess.getConstantDefListRule()); 
            pushFollow(FOLLOW_1);
            iv_ruleConstantDefList=ruleConstantDefList();

            state._fsp--;

             current =iv_ruleConstantDefList; 
            match(input,EOF,FOLLOW_2); 

            }

        }

            catch (RecognitionException re) {
                recover(input,re);
                appendSkippedTokens();
            }
        finally {
        }
        return current;
    }
    // $ANTLR end "entryRuleConstantDefList"


    // $ANTLR start "ruleConstantDefList"
    // InternalFSMDSL.g:369:1: ruleConstantDefList returns [EObject current=null] : ( ( ( (lv_constants_0_0= ruleConstantDef ) ) (otherlv_1= ';' ( (lv_constants_2_0= ruleConstantDef ) ) )* ) otherlv_3= ';' )? ;
    public final EObject ruleConstantDefList() throws RecognitionException {
        EObject current = null;

        Token otherlv_1=null;
        Token otherlv_3=null;
        EObject lv_constants_0_0 = null;

        EObject lv_constants_2_0 = null;



        	enterRule();

        try {
            // InternalFSMDSL.g:375:2: ( ( ( ( (lv_constants_0_0= ruleConstantDef ) ) (otherlv_1= ';' ( (lv_constants_2_0= ruleConstantDef ) ) )* ) otherlv_3= ';' )? )
            // InternalFSMDSL.g:376:2: ( ( ( (lv_constants_0_0= ruleConstantDef ) ) (otherlv_1= ';' ( (lv_constants_2_0= ruleConstantDef ) ) )* ) otherlv_3= ';' )?
            {
            // InternalFSMDSL.g:376:2: ( ( ( (lv_constants_0_0= ruleConstantDef ) ) (otherlv_1= ';' ( (lv_constants_2_0= ruleConstantDef ) ) )* ) otherlv_3= ';' )?
            int alt12=2;
            int LA12_0 = input.LA(1);

            if ( (LA12_0==48) ) {
                alt12=1;
            }
            switch (alt12) {
                case 1 :
                    // InternalFSMDSL.g:377:3: ( ( (lv_constants_0_0= ruleConstantDef ) ) (otherlv_1= ';' ( (lv_constants_2_0= ruleConstantDef ) ) )* ) otherlv_3= ';'
                    {
                    // InternalFSMDSL.g:377:3: ( ( (lv_constants_0_0= ruleConstantDef ) ) (otherlv_1= ';' ( (lv_constants_2_0= ruleConstantDef ) ) )* )
                    // InternalFSMDSL.g:378:4: ( (lv_constants_0_0= ruleConstantDef ) ) (otherlv_1= ';' ( (lv_constants_2_0= ruleConstantDef ) ) )*
                    {
                    // InternalFSMDSL.g:378:4: ( (lv_constants_0_0= ruleConstantDef ) )
                    // InternalFSMDSL.g:379:5: (lv_constants_0_0= ruleConstantDef )
                    {
                    // InternalFSMDSL.g:379:5: (lv_constants_0_0= ruleConstantDef )
                    // InternalFSMDSL.g:380:6: lv_constants_0_0= ruleConstantDef
                    {

                    						newCompositeNode(grammarAccess.getConstantDefListAccess().getConstantsConstantDefParserRuleCall_0_0_0());
                    					
                    pushFollow(FOLLOW_11);
                    lv_constants_0_0=ruleConstantDef();

                    state._fsp--;


                    						if (current==null) {
                    							current = createModelElementForParent(grammarAccess.getConstantDefListRule());
                    						}
                    						add(
                    							current,
                    							"constants",
                    							lv_constants_0_0,
                    							"com.cburch.logisim.statemachine.FSMDSL.ConstantDef");
                    						afterParserOrEnumRuleCall();
                    					

                    }


                    }

                    // InternalFSMDSL.g:397:4: (otherlv_1= ';' ( (lv_constants_2_0= ruleConstantDef ) ) )*
                    loop11:
                    do {
                        int alt11=2;
                        int LA11_0 = input.LA(1);

                        if ( (LA11_0==16) ) {
                            int LA11_1 = input.LA(2);

                            if ( (LA11_1==48) ) {
                                alt11=1;
                            }


                        }


                        switch (alt11) {
                    	case 1 :
                    	    // InternalFSMDSL.g:398:5: otherlv_1= ';' ( (lv_constants_2_0= ruleConstantDef ) )
                    	    {
                    	    otherlv_1=(Token)match(input,16,FOLLOW_5); 

                    	    					newLeafNode(otherlv_1, grammarAccess.getConstantDefListAccess().getSemicolonKeyword_0_1_0());
                    	    				
                    	    // InternalFSMDSL.g:402:5: ( (lv_constants_2_0= ruleConstantDef ) )
                    	    // InternalFSMDSL.g:403:6: (lv_constants_2_0= ruleConstantDef )
                    	    {
                    	    // InternalFSMDSL.g:403:6: (lv_constants_2_0= ruleConstantDef )
                    	    // InternalFSMDSL.g:404:7: lv_constants_2_0= ruleConstantDef
                    	    {

                    	    							newCompositeNode(grammarAccess.getConstantDefListAccess().getConstantsConstantDefParserRuleCall_0_1_1_0());
                    	    						
                    	    pushFollow(FOLLOW_11);
                    	    lv_constants_2_0=ruleConstantDef();

                    	    state._fsp--;


                    	    							if (current==null) {
                    	    								current = createModelElementForParent(grammarAccess.getConstantDefListRule());
                    	    							}
                    	    							add(
                    	    								current,
                    	    								"constants",
                    	    								lv_constants_2_0,
                    	    								"com.cburch.logisim.statemachine.FSMDSL.ConstantDef");
                    	    							afterParserOrEnumRuleCall();
                    	    						

                    	    }


                    	    }


                    	    }
                    	    break;

                    	default :
                    	    break loop11;
                        }
                    } while (true);


                    }

                    otherlv_3=(Token)match(input,16,FOLLOW_2); 

                    			newLeafNode(otherlv_3, grammarAccess.getConstantDefListAccess().getSemicolonKeyword_1());
                    		

                    }
                    break;

            }


            }


            	leaveRule();

        }

            catch (RecognitionException re) {
                recover(input,re);
                appendSkippedTokens();
            }
        finally {
        }
        return current;
    }
    // $ANTLR end "ruleConstantDefList"


    // $ANTLR start "entryRulePredicateStmt"
    // InternalFSMDSL.g:431:1: entryRulePredicateStmt returns [EObject current=null] : iv_rulePredicateStmt= rulePredicateStmt EOF ;
    public final EObject entryRulePredicateStmt() throws RecognitionException {
        EObject current = null;

        EObject iv_rulePredicateStmt = null;


        try {
            // InternalFSMDSL.g:431:54: (iv_rulePredicateStmt= rulePredicateStmt EOF )
            // InternalFSMDSL.g:432:2: iv_rulePredicateStmt= rulePredicateStmt EOF
            {
             newCompositeNode(grammarAccess.getPredicateStmtRule()); 
            pushFollow(FOLLOW_1);
            iv_rulePredicateStmt=rulePredicateStmt();

            state._fsp--;

             current =iv_rulePredicateStmt; 
            match(input,EOF,FOLLOW_2); 

            }

        }

            catch (RecognitionException re) {
                recover(input,re);
                appendSkippedTokens();
            }
        finally {
        }
        return current;
    }
    // $ANTLR end "entryRulePredicateStmt"


    // $ANTLR start "rulePredicateStmt"
    // InternalFSMDSL.g:438:1: rulePredicateStmt returns [EObject current=null] : (otherlv_0= '[' ( ( (lv_cst_1_0= ruleConstantDef ) ) (otherlv_2= ',' ( (lv_cst_3_0= ruleConstantDef ) ) )* )? otherlv_4= ']' otherlv_5= '[' ( ( (lv_in_6_0= ruleShortInputPort ) ) (otherlv_7= ',' ( (lv_in_8_0= ruleShortInputPort ) ) )* )? otherlv_9= ']' ( (lv_predicate_10_0= rulePredicate ) ) otherlv_11= ';' ) ;
    public final EObject rulePredicateStmt() throws RecognitionException {
        EObject current = null;

        Token otherlv_0=null;
        Token otherlv_2=null;
        Token otherlv_4=null;
        Token otherlv_5=null;
        Token otherlv_7=null;
        Token otherlv_9=null;
        Token otherlv_11=null;
        EObject lv_cst_1_0 = null;

        EObject lv_cst_3_0 = null;

        EObject lv_in_6_0 = null;

        EObject lv_in_8_0 = null;

        EObject lv_predicate_10_0 = null;



        	enterRule();

        try {
            // InternalFSMDSL.g:444:2: ( (otherlv_0= '[' ( ( (lv_cst_1_0= ruleConstantDef ) ) (otherlv_2= ',' ( (lv_cst_3_0= ruleConstantDef ) ) )* )? otherlv_4= ']' otherlv_5= '[' ( ( (lv_in_6_0= ruleShortInputPort ) ) (otherlv_7= ',' ( (lv_in_8_0= ruleShortInputPort ) ) )* )? otherlv_9= ']' ( (lv_predicate_10_0= rulePredicate ) ) otherlv_11= ';' ) )
            // InternalFSMDSL.g:445:2: (otherlv_0= '[' ( ( (lv_cst_1_0= ruleConstantDef ) ) (otherlv_2= ',' ( (lv_cst_3_0= ruleConstantDef ) ) )* )? otherlv_4= ']' otherlv_5= '[' ( ( (lv_in_6_0= ruleShortInputPort ) ) (otherlv_7= ',' ( (lv_in_8_0= ruleShortInputPort ) ) )* )? otherlv_9= ']' ( (lv_predicate_10_0= rulePredicate ) ) otherlv_11= ';' )
            {
            // InternalFSMDSL.g:445:2: (otherlv_0= '[' ( ( (lv_cst_1_0= ruleConstantDef ) ) (otherlv_2= ',' ( (lv_cst_3_0= ruleConstantDef ) ) )* )? otherlv_4= ']' otherlv_5= '[' ( ( (lv_in_6_0= ruleShortInputPort ) ) (otherlv_7= ',' ( (lv_in_8_0= ruleShortInputPort ) ) )* )? otherlv_9= ']' ( (lv_predicate_10_0= rulePredicate ) ) otherlv_11= ';' )
            // InternalFSMDSL.g:446:3: otherlv_0= '[' ( ( (lv_cst_1_0= ruleConstantDef ) ) (otherlv_2= ',' ( (lv_cst_3_0= ruleConstantDef ) ) )* )? otherlv_4= ']' otherlv_5= '[' ( ( (lv_in_6_0= ruleShortInputPort ) ) (otherlv_7= ',' ( (lv_in_8_0= ruleShortInputPort ) ) )* )? otherlv_9= ']' ( (lv_predicate_10_0= rulePredicate ) ) otherlv_11= ';'
            {
            otherlv_0=(Token)match(input,13,FOLLOW_3); 

            			newLeafNode(otherlv_0, grammarAccess.getPredicateStmtAccess().getLeftSquareBracketKeyword_0());
            		
            // InternalFSMDSL.g:450:3: ( ( (lv_cst_1_0= ruleConstantDef ) ) (otherlv_2= ',' ( (lv_cst_3_0= ruleConstantDef ) ) )* )?
            int alt14=2;
            int LA14_0 = input.LA(1);

            if ( (LA14_0==48) ) {
                alt14=1;
            }
            switch (alt14) {
                case 1 :
                    // InternalFSMDSL.g:451:4: ( (lv_cst_1_0= ruleConstantDef ) ) (otherlv_2= ',' ( (lv_cst_3_0= ruleConstantDef ) ) )*
                    {
                    // InternalFSMDSL.g:451:4: ( (lv_cst_1_0= ruleConstantDef ) )
                    // InternalFSMDSL.g:452:5: (lv_cst_1_0= ruleConstantDef )
                    {
                    // InternalFSMDSL.g:452:5: (lv_cst_1_0= ruleConstantDef )
                    // InternalFSMDSL.g:453:6: lv_cst_1_0= ruleConstantDef
                    {

                    						newCompositeNode(grammarAccess.getPredicateStmtAccess().getCstConstantDefParserRuleCall_1_0_0());
                    					
                    pushFollow(FOLLOW_4);
                    lv_cst_1_0=ruleConstantDef();

                    state._fsp--;


                    						if (current==null) {
                    							current = createModelElementForParent(grammarAccess.getPredicateStmtRule());
                    						}
                    						add(
                    							current,
                    							"cst",
                    							lv_cst_1_0,
                    							"com.cburch.logisim.statemachine.FSMDSL.ConstantDef");
                    						afterParserOrEnumRuleCall();
                    					

                    }


                    }

                    // InternalFSMDSL.g:470:4: (otherlv_2= ',' ( (lv_cst_3_0= ruleConstantDef ) ) )*
                    loop13:
                    do {
                        int alt13=2;
                        int LA13_0 = input.LA(1);

                        if ( (LA13_0==14) ) {
                            alt13=1;
                        }


                        switch (alt13) {
                    	case 1 :
                    	    // InternalFSMDSL.g:471:5: otherlv_2= ',' ( (lv_cst_3_0= ruleConstantDef ) )
                    	    {
                    	    otherlv_2=(Token)match(input,14,FOLLOW_5); 

                    	    					newLeafNode(otherlv_2, grammarAccess.getPredicateStmtAccess().getCommaKeyword_1_1_0());
                    	    				
                    	    // InternalFSMDSL.g:475:5: ( (lv_cst_3_0= ruleConstantDef ) )
                    	    // InternalFSMDSL.g:476:6: (lv_cst_3_0= ruleConstantDef )
                    	    {
                    	    // InternalFSMDSL.g:476:6: (lv_cst_3_0= ruleConstantDef )
                    	    // InternalFSMDSL.g:477:7: lv_cst_3_0= ruleConstantDef
                    	    {

                    	    							newCompositeNode(grammarAccess.getPredicateStmtAccess().getCstConstantDefParserRuleCall_1_1_1_0());
                    	    						
                    	    pushFollow(FOLLOW_4);
                    	    lv_cst_3_0=ruleConstantDef();

                    	    state._fsp--;


                    	    							if (current==null) {
                    	    								current = createModelElementForParent(grammarAccess.getPredicateStmtRule());
                    	    							}
                    	    							add(
                    	    								current,
                    	    								"cst",
                    	    								lv_cst_3_0,
                    	    								"com.cburch.logisim.statemachine.FSMDSL.ConstantDef");
                    	    							afterParserOrEnumRuleCall();
                    	    						

                    	    }


                    	    }


                    	    }
                    	    break;

                    	default :
                    	    break loop13;
                        }
                    } while (true);


                    }
                    break;

            }

            otherlv_4=(Token)match(input,15,FOLLOW_6); 

            			newLeafNode(otherlv_4, grammarAccess.getPredicateStmtAccess().getRightSquareBracketKeyword_2());
            		
            otherlv_5=(Token)match(input,13,FOLLOW_7); 

            			newLeafNode(otherlv_5, grammarAccess.getPredicateStmtAccess().getLeftSquareBracketKeyword_3());
            		
            // InternalFSMDSL.g:504:3: ( ( (lv_in_6_0= ruleShortInputPort ) ) (otherlv_7= ',' ( (lv_in_8_0= ruleShortInputPort ) ) )* )?
            int alt16=2;
            int LA16_0 = input.LA(1);

            if ( (LA16_0==RULE_ID) ) {
                alt16=1;
            }
            switch (alt16) {
                case 1 :
                    // InternalFSMDSL.g:505:4: ( (lv_in_6_0= ruleShortInputPort ) ) (otherlv_7= ',' ( (lv_in_8_0= ruleShortInputPort ) ) )*
                    {
                    // InternalFSMDSL.g:505:4: ( (lv_in_6_0= ruleShortInputPort ) )
                    // InternalFSMDSL.g:506:5: (lv_in_6_0= ruleShortInputPort )
                    {
                    // InternalFSMDSL.g:506:5: (lv_in_6_0= ruleShortInputPort )
                    // InternalFSMDSL.g:507:6: lv_in_6_0= ruleShortInputPort
                    {

                    						newCompositeNode(grammarAccess.getPredicateStmtAccess().getInShortInputPortParserRuleCall_4_0_0());
                    					
                    pushFollow(FOLLOW_4);
                    lv_in_6_0=ruleShortInputPort();

                    state._fsp--;


                    						if (current==null) {
                    							current = createModelElementForParent(grammarAccess.getPredicateStmtRule());
                    						}
                    						add(
                    							current,
                    							"in",
                    							lv_in_6_0,
                    							"com.cburch.logisim.statemachine.FSMDSL.ShortInputPort");
                    						afterParserOrEnumRuleCall();
                    					

                    }


                    }

                    // InternalFSMDSL.g:524:4: (otherlv_7= ',' ( (lv_in_8_0= ruleShortInputPort ) ) )*
                    loop15:
                    do {
                        int alt15=2;
                        int LA15_0 = input.LA(1);

                        if ( (LA15_0==14) ) {
                            alt15=1;
                        }


                        switch (alt15) {
                    	case 1 :
                    	    // InternalFSMDSL.g:525:5: otherlv_7= ',' ( (lv_in_8_0= ruleShortInputPort ) )
                    	    {
                    	    otherlv_7=(Token)match(input,14,FOLLOW_8); 

                    	    					newLeafNode(otherlv_7, grammarAccess.getPredicateStmtAccess().getCommaKeyword_4_1_0());
                    	    				
                    	    // InternalFSMDSL.g:529:5: ( (lv_in_8_0= ruleShortInputPort ) )
                    	    // InternalFSMDSL.g:530:6: (lv_in_8_0= ruleShortInputPort )
                    	    {
                    	    // InternalFSMDSL.g:530:6: (lv_in_8_0= ruleShortInputPort )
                    	    // InternalFSMDSL.g:531:7: lv_in_8_0= ruleShortInputPort
                    	    {

                    	    							newCompositeNode(grammarAccess.getPredicateStmtAccess().getInShortInputPortParserRuleCall_4_1_1_0());
                    	    						
                    	    pushFollow(FOLLOW_4);
                    	    lv_in_8_0=ruleShortInputPort();

                    	    state._fsp--;


                    	    							if (current==null) {
                    	    								current = createModelElementForParent(grammarAccess.getPredicateStmtRule());
                    	    							}
                    	    							add(
                    	    								current,
                    	    								"in",
                    	    								lv_in_8_0,
                    	    								"com.cburch.logisim.statemachine.FSMDSL.ShortInputPort");
                    	    							afterParserOrEnumRuleCall();
                    	    						

                    	    }


                    	    }


                    	    }
                    	    break;

                    	default :
                    	    break loop15;
                        }
                    } while (true);


                    }
                    break;

            }

            otherlv_9=(Token)match(input,15,FOLLOW_12); 

            			newLeafNode(otherlv_9, grammarAccess.getPredicateStmtAccess().getRightSquareBracketKeyword_5());
            		
            // InternalFSMDSL.g:554:3: ( (lv_predicate_10_0= rulePredicate ) )
            // InternalFSMDSL.g:555:4: (lv_predicate_10_0= rulePredicate )
            {
            // InternalFSMDSL.g:555:4: (lv_predicate_10_0= rulePredicate )
            // InternalFSMDSL.g:556:5: lv_predicate_10_0= rulePredicate
            {

            					newCompositeNode(grammarAccess.getPredicateStmtAccess().getPredicatePredicateParserRuleCall_6_0());
            				
            pushFollow(FOLLOW_11);
            lv_predicate_10_0=rulePredicate();

            state._fsp--;


            					if (current==null) {
            						current = createModelElementForParent(grammarAccess.getPredicateStmtRule());
            					}
            					set(
            						current,
            						"predicate",
            						lv_predicate_10_0,
            						"com.cburch.logisim.statemachine.FSMDSL.Predicate");
            					afterParserOrEnumRuleCall();
            				

            }


            }

            otherlv_11=(Token)match(input,16,FOLLOW_2); 

            			newLeafNode(otherlv_11, grammarAccess.getPredicateStmtAccess().getSemicolonKeyword_7());
            		

            }


            }


            	leaveRule();

        }

            catch (RecognitionException re) {
                recover(input,re);
                appendSkippedTokens();
            }
        finally {
        }
        return current;
    }
    // $ANTLR end "rulePredicateStmt"


    // $ANTLR start "entryRuleEQNSpec"
    // InternalFSMDSL.g:581:1: entryRuleEQNSpec returns [EObject current=null] : iv_ruleEQNSpec= ruleEQNSpec EOF ;
    public final EObject entryRuleEQNSpec() throws RecognitionException {
        EObject current = null;

        EObject iv_ruleEQNSpec = null;


        try {
            // InternalFSMDSL.g:581:48: (iv_ruleEQNSpec= ruleEQNSpec EOF )
            // InternalFSMDSL.g:582:2: iv_ruleEQNSpec= ruleEQNSpec EOF
            {
             newCompositeNode(grammarAccess.getEQNSpecRule()); 
            pushFollow(FOLLOW_1);
            iv_ruleEQNSpec=ruleEQNSpec();

            state._fsp--;

             current =iv_ruleEQNSpec; 
            match(input,EOF,FOLLOW_2); 

            }

        }

            catch (RecognitionException re) {
                recover(input,re);
                appendSkippedTokens();
            }
        finally {
        }
        return current;
    }
    // $ANTLR end "entryRuleEQNSpec"


    // $ANTLR start "ruleEQNSpec"
    // InternalFSMDSL.g:588:1: ruleEQNSpec returns [EObject current=null] : (otherlv_0= 'INORDER' otherlv_1= '=' ( ( (lv_in_2_0= ruleShortInputPort ) ) (otherlv_3= ',' ( (lv_in_4_0= ruleShortInputPort ) ) )* )? otherlv_5= ';' otherlv_6= 'OUTORDER' otherlv_7= '=' ( ( (lv_outNames_8_0= RULE_ID ) ) (otherlv_9= ',' ( (lv_outNames_10_0= RULE_ID ) ) )* )? otherlv_11= ';' ( (lv_eqns_12_0= ruleEQNAssignement ) )+ ) ;
    public final EObject ruleEQNSpec() throws RecognitionException {
        EObject current = null;

        Token otherlv_0=null;
        Token otherlv_1=null;
        Token otherlv_3=null;
        Token otherlv_5=null;
        Token otherlv_6=null;
        Token otherlv_7=null;
        Token lv_outNames_8_0=null;
        Token otherlv_9=null;
        Token lv_outNames_10_0=null;
        Token otherlv_11=null;
        EObject lv_in_2_0 = null;

        EObject lv_in_4_0 = null;

        EObject lv_eqns_12_0 = null;



        	enterRule();

        try {
            // InternalFSMDSL.g:594:2: ( (otherlv_0= 'INORDER' otherlv_1= '=' ( ( (lv_in_2_0= ruleShortInputPort ) ) (otherlv_3= ',' ( (lv_in_4_0= ruleShortInputPort ) ) )* )? otherlv_5= ';' otherlv_6= 'OUTORDER' otherlv_7= '=' ( ( (lv_outNames_8_0= RULE_ID ) ) (otherlv_9= ',' ( (lv_outNames_10_0= RULE_ID ) ) )* )? otherlv_11= ';' ( (lv_eqns_12_0= ruleEQNAssignement ) )+ ) )
            // InternalFSMDSL.g:595:2: (otherlv_0= 'INORDER' otherlv_1= '=' ( ( (lv_in_2_0= ruleShortInputPort ) ) (otherlv_3= ',' ( (lv_in_4_0= ruleShortInputPort ) ) )* )? otherlv_5= ';' otherlv_6= 'OUTORDER' otherlv_7= '=' ( ( (lv_outNames_8_0= RULE_ID ) ) (otherlv_9= ',' ( (lv_outNames_10_0= RULE_ID ) ) )* )? otherlv_11= ';' ( (lv_eqns_12_0= ruleEQNAssignement ) )+ )
            {
            // InternalFSMDSL.g:595:2: (otherlv_0= 'INORDER' otherlv_1= '=' ( ( (lv_in_2_0= ruleShortInputPort ) ) (otherlv_3= ',' ( (lv_in_4_0= ruleShortInputPort ) ) )* )? otherlv_5= ';' otherlv_6= 'OUTORDER' otherlv_7= '=' ( ( (lv_outNames_8_0= RULE_ID ) ) (otherlv_9= ',' ( (lv_outNames_10_0= RULE_ID ) ) )* )? otherlv_11= ';' ( (lv_eqns_12_0= ruleEQNAssignement ) )+ )
            // InternalFSMDSL.g:596:3: otherlv_0= 'INORDER' otherlv_1= '=' ( ( (lv_in_2_0= ruleShortInputPort ) ) (otherlv_3= ',' ( (lv_in_4_0= ruleShortInputPort ) ) )* )? otherlv_5= ';' otherlv_6= 'OUTORDER' otherlv_7= '=' ( ( (lv_outNames_8_0= RULE_ID ) ) (otherlv_9= ',' ( (lv_outNames_10_0= RULE_ID ) ) )* )? otherlv_11= ';' ( (lv_eqns_12_0= ruleEQNAssignement ) )+
            {
            otherlv_0=(Token)match(input,17,FOLLOW_13); 

            			newLeafNode(otherlv_0, grammarAccess.getEQNSpecAccess().getINORDERKeyword_0());
            		
            otherlv_1=(Token)match(input,18,FOLLOW_14); 

            			newLeafNode(otherlv_1, grammarAccess.getEQNSpecAccess().getEqualsSignKeyword_1());
            		
            // InternalFSMDSL.g:604:3: ( ( (lv_in_2_0= ruleShortInputPort ) ) (otherlv_3= ',' ( (lv_in_4_0= ruleShortInputPort ) ) )* )?
            int alt18=2;
            int LA18_0 = input.LA(1);

            if ( (LA18_0==RULE_ID) ) {
                alt18=1;
            }
            switch (alt18) {
                case 1 :
                    // InternalFSMDSL.g:605:4: ( (lv_in_2_0= ruleShortInputPort ) ) (otherlv_3= ',' ( (lv_in_4_0= ruleShortInputPort ) ) )*
                    {
                    // InternalFSMDSL.g:605:4: ( (lv_in_2_0= ruleShortInputPort ) )
                    // InternalFSMDSL.g:606:5: (lv_in_2_0= ruleShortInputPort )
                    {
                    // InternalFSMDSL.g:606:5: (lv_in_2_0= ruleShortInputPort )
                    // InternalFSMDSL.g:607:6: lv_in_2_0= ruleShortInputPort
                    {

                    						newCompositeNode(grammarAccess.getEQNSpecAccess().getInShortInputPortParserRuleCall_2_0_0());
                    					
                    pushFollow(FOLLOW_15);
                    lv_in_2_0=ruleShortInputPort();

                    state._fsp--;


                    						if (current==null) {
                    							current = createModelElementForParent(grammarAccess.getEQNSpecRule());
                    						}
                    						add(
                    							current,
                    							"in",
                    							lv_in_2_0,
                    							"com.cburch.logisim.statemachine.FSMDSL.ShortInputPort");
                    						afterParserOrEnumRuleCall();
                    					

                    }


                    }

                    // InternalFSMDSL.g:624:4: (otherlv_3= ',' ( (lv_in_4_0= ruleShortInputPort ) ) )*
                    loop17:
                    do {
                        int alt17=2;
                        int LA17_0 = input.LA(1);

                        if ( (LA17_0==14) ) {
                            alt17=1;
                        }


                        switch (alt17) {
                    	case 1 :
                    	    // InternalFSMDSL.g:625:5: otherlv_3= ',' ( (lv_in_4_0= ruleShortInputPort ) )
                    	    {
                    	    otherlv_3=(Token)match(input,14,FOLLOW_8); 

                    	    					newLeafNode(otherlv_3, grammarAccess.getEQNSpecAccess().getCommaKeyword_2_1_0());
                    	    				
                    	    // InternalFSMDSL.g:629:5: ( (lv_in_4_0= ruleShortInputPort ) )
                    	    // InternalFSMDSL.g:630:6: (lv_in_4_0= ruleShortInputPort )
                    	    {
                    	    // InternalFSMDSL.g:630:6: (lv_in_4_0= ruleShortInputPort )
                    	    // InternalFSMDSL.g:631:7: lv_in_4_0= ruleShortInputPort
                    	    {

                    	    							newCompositeNode(grammarAccess.getEQNSpecAccess().getInShortInputPortParserRuleCall_2_1_1_0());
                    	    						
                    	    pushFollow(FOLLOW_15);
                    	    lv_in_4_0=ruleShortInputPort();

                    	    state._fsp--;


                    	    							if (current==null) {
                    	    								current = createModelElementForParent(grammarAccess.getEQNSpecRule());
                    	    							}
                    	    							add(
                    	    								current,
                    	    								"in",
                    	    								lv_in_4_0,
                    	    								"com.cburch.logisim.statemachine.FSMDSL.ShortInputPort");
                    	    							afterParserOrEnumRuleCall();
                    	    						

                    	    }


                    	    }


                    	    }
                    	    break;

                    	default :
                    	    break loop17;
                        }
                    } while (true);


                    }
                    break;

            }

            otherlv_5=(Token)match(input,16,FOLLOW_16); 

            			newLeafNode(otherlv_5, grammarAccess.getEQNSpecAccess().getSemicolonKeyword_3());
            		
            otherlv_6=(Token)match(input,19,FOLLOW_13); 

            			newLeafNode(otherlv_6, grammarAccess.getEQNSpecAccess().getOUTORDERKeyword_4());
            		
            otherlv_7=(Token)match(input,18,FOLLOW_14); 

            			newLeafNode(otherlv_7, grammarAccess.getEQNSpecAccess().getEqualsSignKeyword_5());
            		
            // InternalFSMDSL.g:662:3: ( ( (lv_outNames_8_0= RULE_ID ) ) (otherlv_9= ',' ( (lv_outNames_10_0= RULE_ID ) ) )* )?
            int alt20=2;
            int LA20_0 = input.LA(1);

            if ( (LA20_0==RULE_ID) ) {
                alt20=1;
            }
            switch (alt20) {
                case 1 :
                    // InternalFSMDSL.g:663:4: ( (lv_outNames_8_0= RULE_ID ) ) (otherlv_9= ',' ( (lv_outNames_10_0= RULE_ID ) ) )*
                    {
                    // InternalFSMDSL.g:663:4: ( (lv_outNames_8_0= RULE_ID ) )
                    // InternalFSMDSL.g:664:5: (lv_outNames_8_0= RULE_ID )
                    {
                    // InternalFSMDSL.g:664:5: (lv_outNames_8_0= RULE_ID )
                    // InternalFSMDSL.g:665:6: lv_outNames_8_0= RULE_ID
                    {
                    lv_outNames_8_0=(Token)match(input,RULE_ID,FOLLOW_15); 

                    						newLeafNode(lv_outNames_8_0, grammarAccess.getEQNSpecAccess().getOutNamesIDTerminalRuleCall_6_0_0());
                    					

                    						if (current==null) {
                    							current = createModelElement(grammarAccess.getEQNSpecRule());
                    						}
                    						addWithLastConsumed(
                    							current,
                    							"outNames",
                    							lv_outNames_8_0,
                    							"org.eclipse.xtext.common.Terminals.ID");
                    					

                    }


                    }

                    // InternalFSMDSL.g:681:4: (otherlv_9= ',' ( (lv_outNames_10_0= RULE_ID ) ) )*
                    loop19:
                    do {
                        int alt19=2;
                        int LA19_0 = input.LA(1);

                        if ( (LA19_0==14) ) {
                            alt19=1;
                        }


                        switch (alt19) {
                    	case 1 :
                    	    // InternalFSMDSL.g:682:5: otherlv_9= ',' ( (lv_outNames_10_0= RULE_ID ) )
                    	    {
                    	    otherlv_9=(Token)match(input,14,FOLLOW_8); 

                    	    					newLeafNode(otherlv_9, grammarAccess.getEQNSpecAccess().getCommaKeyword_6_1_0());
                    	    				
                    	    // InternalFSMDSL.g:686:5: ( (lv_outNames_10_0= RULE_ID ) )
                    	    // InternalFSMDSL.g:687:6: (lv_outNames_10_0= RULE_ID )
                    	    {
                    	    // InternalFSMDSL.g:687:6: (lv_outNames_10_0= RULE_ID )
                    	    // InternalFSMDSL.g:688:7: lv_outNames_10_0= RULE_ID
                    	    {
                    	    lv_outNames_10_0=(Token)match(input,RULE_ID,FOLLOW_15); 

                    	    							newLeafNode(lv_outNames_10_0, grammarAccess.getEQNSpecAccess().getOutNamesIDTerminalRuleCall_6_1_1_0());
                    	    						

                    	    							if (current==null) {
                    	    								current = createModelElement(grammarAccess.getEQNSpecRule());
                    	    							}
                    	    							addWithLastConsumed(
                    	    								current,
                    	    								"outNames",
                    	    								lv_outNames_10_0,
                    	    								"org.eclipse.xtext.common.Terminals.ID");
                    	    						

                    	    }


                    	    }


                    	    }
                    	    break;

                    	default :
                    	    break loop19;
                        }
                    } while (true);


                    }
                    break;

            }

            otherlv_11=(Token)match(input,16,FOLLOW_8); 

            			newLeafNode(otherlv_11, grammarAccess.getEQNSpecAccess().getSemicolonKeyword_7());
            		
            // InternalFSMDSL.g:710:3: ( (lv_eqns_12_0= ruleEQNAssignement ) )+
            int cnt21=0;
            loop21:
            do {
                int alt21=2;
                int LA21_0 = input.LA(1);

                if ( (LA21_0==RULE_ID) ) {
                    alt21=1;
                }


                switch (alt21) {
            	case 1 :
            	    // InternalFSMDSL.g:711:4: (lv_eqns_12_0= ruleEQNAssignement )
            	    {
            	    // InternalFSMDSL.g:711:4: (lv_eqns_12_0= ruleEQNAssignement )
            	    // InternalFSMDSL.g:712:5: lv_eqns_12_0= ruleEQNAssignement
            	    {

            	    					newCompositeNode(grammarAccess.getEQNSpecAccess().getEqnsEQNAssignementParserRuleCall_8_0());
            	    				
            	    pushFollow(FOLLOW_10);
            	    lv_eqns_12_0=ruleEQNAssignement();

            	    state._fsp--;


            	    					if (current==null) {
            	    						current = createModelElementForParent(grammarAccess.getEQNSpecRule());
            	    					}
            	    					add(
            	    						current,
            	    						"eqns",
            	    						lv_eqns_12_0,
            	    						"com.cburch.logisim.statemachine.FSMDSL.EQNAssignement");
            	    					afterParserOrEnumRuleCall();
            	    				

            	    }


            	    }
            	    break;

            	default :
            	    if ( cnt21 >= 1 ) break loop21;
                        EarlyExitException eee =
                            new EarlyExitException(21, input);
                        throw eee;
                }
                cnt21++;
            } while (true);


            }


            }


            	leaveRule();

        }

            catch (RecognitionException re) {
                recover(input,re);
                appendSkippedTokens();
            }
        finally {
        }
        return current;
    }
    // $ANTLR end "ruleEQNSpec"


    // $ANTLR start "entryRuleEQNAssignement"
    // InternalFSMDSL.g:733:1: entryRuleEQNAssignement returns [EObject current=null] : iv_ruleEQNAssignement= ruleEQNAssignement EOF ;
    public final EObject entryRuleEQNAssignement() throws RecognitionException {
        EObject current = null;

        EObject iv_ruleEQNAssignement = null;


        try {
            // InternalFSMDSL.g:733:55: (iv_ruleEQNAssignement= ruleEQNAssignement EOF )
            // InternalFSMDSL.g:734:2: iv_ruleEQNAssignement= ruleEQNAssignement EOF
            {
             newCompositeNode(grammarAccess.getEQNAssignementRule()); 
            pushFollow(FOLLOW_1);
            iv_ruleEQNAssignement=ruleEQNAssignement();

            state._fsp--;

             current =iv_ruleEQNAssignement; 
            match(input,EOF,FOLLOW_2); 

            }

        }

            catch (RecognitionException re) {
                recover(input,re);
                appendSkippedTokens();
            }
        finally {
        }
        return current;
    }
    // $ANTLR end "entryRuleEQNAssignement"


    // $ANTLR start "ruleEQNAssignement"
    // InternalFSMDSL.g:740:1: ruleEQNAssignement returns [EObject current=null] : ( ( (lv_lhs_0_0= ruleShortOutputPort ) ) otherlv_1= '=' ( (lv_rhs_2_0= ruleOrPla ) ) otherlv_3= ';' ) ;
    public final EObject ruleEQNAssignement() throws RecognitionException {
        EObject current = null;

        Token otherlv_1=null;
        Token otherlv_3=null;
        EObject lv_lhs_0_0 = null;

        EObject lv_rhs_2_0 = null;



        	enterRule();

        try {
            // InternalFSMDSL.g:746:2: ( ( ( (lv_lhs_0_0= ruleShortOutputPort ) ) otherlv_1= '=' ( (lv_rhs_2_0= ruleOrPla ) ) otherlv_3= ';' ) )
            // InternalFSMDSL.g:747:2: ( ( (lv_lhs_0_0= ruleShortOutputPort ) ) otherlv_1= '=' ( (lv_rhs_2_0= ruleOrPla ) ) otherlv_3= ';' )
            {
            // InternalFSMDSL.g:747:2: ( ( (lv_lhs_0_0= ruleShortOutputPort ) ) otherlv_1= '=' ( (lv_rhs_2_0= ruleOrPla ) ) otherlv_3= ';' )
            // InternalFSMDSL.g:748:3: ( (lv_lhs_0_0= ruleShortOutputPort ) ) otherlv_1= '=' ( (lv_rhs_2_0= ruleOrPla ) ) otherlv_3= ';'
            {
            // InternalFSMDSL.g:748:3: ( (lv_lhs_0_0= ruleShortOutputPort ) )
            // InternalFSMDSL.g:749:4: (lv_lhs_0_0= ruleShortOutputPort )
            {
            // InternalFSMDSL.g:749:4: (lv_lhs_0_0= ruleShortOutputPort )
            // InternalFSMDSL.g:750:5: lv_lhs_0_0= ruleShortOutputPort
            {

            					newCompositeNode(grammarAccess.getEQNAssignementAccess().getLhsShortOutputPortParserRuleCall_0_0());
            				
            pushFollow(FOLLOW_13);
            lv_lhs_0_0=ruleShortOutputPort();

            state._fsp--;


            					if (current==null) {
            						current = createModelElementForParent(grammarAccess.getEQNAssignementRule());
            					}
            					set(
            						current,
            						"lhs",
            						lv_lhs_0_0,
            						"com.cburch.logisim.statemachine.FSMDSL.ShortOutputPort");
            					afterParserOrEnumRuleCall();
            				

            }


            }

            otherlv_1=(Token)match(input,18,FOLLOW_17); 

            			newLeafNode(otherlv_1, grammarAccess.getEQNAssignementAccess().getEqualsSignKeyword_1());
            		
            // InternalFSMDSL.g:771:3: ( (lv_rhs_2_0= ruleOrPla ) )
            // InternalFSMDSL.g:772:4: (lv_rhs_2_0= ruleOrPla )
            {
            // InternalFSMDSL.g:772:4: (lv_rhs_2_0= ruleOrPla )
            // InternalFSMDSL.g:773:5: lv_rhs_2_0= ruleOrPla
            {

            					newCompositeNode(grammarAccess.getEQNAssignementAccess().getRhsOrPlaParserRuleCall_2_0());
            				
            pushFollow(FOLLOW_11);
            lv_rhs_2_0=ruleOrPla();

            state._fsp--;


            					if (current==null) {
            						current = createModelElementForParent(grammarAccess.getEQNAssignementRule());
            					}
            					set(
            						current,
            						"rhs",
            						lv_rhs_2_0,
            						"com.cburch.logisim.statemachine.FSMDSL.OrPla");
            					afterParserOrEnumRuleCall();
            				

            }


            }

            otherlv_3=(Token)match(input,16,FOLLOW_2); 

            			newLeafNode(otherlv_3, grammarAccess.getEQNAssignementAccess().getSemicolonKeyword_3());
            		

            }


            }


            	leaveRule();

        }

            catch (RecognitionException re) {
                recover(input,re);
                appendSkippedTokens();
            }
        finally {
        }
        return current;
    }
    // $ANTLR end "ruleEQNAssignement"


    // $ANTLR start "entryRuleOrPla"
    // InternalFSMDSL.g:798:1: entryRuleOrPla returns [EObject current=null] : iv_ruleOrPla= ruleOrPla EOF ;
    public final EObject entryRuleOrPla() throws RecognitionException {
        EObject current = null;

        EObject iv_ruleOrPla = null;


        try {
            // InternalFSMDSL.g:798:46: (iv_ruleOrPla= ruleOrPla EOF )
            // InternalFSMDSL.g:799:2: iv_ruleOrPla= ruleOrPla EOF
            {
             newCompositeNode(grammarAccess.getOrPlaRule()); 
            pushFollow(FOLLOW_1);
            iv_ruleOrPla=ruleOrPla();

            state._fsp--;

             current =iv_ruleOrPla; 
            match(input,EOF,FOLLOW_2); 

            }

        }

            catch (RecognitionException re) {
                recover(input,re);
                appendSkippedTokens();
            }
        finally {
        }
        return current;
    }
    // $ANTLR end "entryRuleOrPla"


    // $ANTLR start "ruleOrPla"
    // InternalFSMDSL.g:805:1: ruleOrPla returns [EObject current=null] : (this_AndPla_0= ruleAndPla ( () otherlv_2= '+' ( (lv_args_3_0= ruleAnd ) ) )* ) ;
    public final EObject ruleOrPla() throws RecognitionException {
        EObject current = null;

        Token otherlv_2=null;
        EObject this_AndPla_0 = null;

        EObject lv_args_3_0 = null;



        	enterRule();

        try {
            // InternalFSMDSL.g:811:2: ( (this_AndPla_0= ruleAndPla ( () otherlv_2= '+' ( (lv_args_3_0= ruleAnd ) ) )* ) )
            // InternalFSMDSL.g:812:2: (this_AndPla_0= ruleAndPla ( () otherlv_2= '+' ( (lv_args_3_0= ruleAnd ) ) )* )
            {
            // InternalFSMDSL.g:812:2: (this_AndPla_0= ruleAndPla ( () otherlv_2= '+' ( (lv_args_3_0= ruleAnd ) ) )* )
            // InternalFSMDSL.g:813:3: this_AndPla_0= ruleAndPla ( () otherlv_2= '+' ( (lv_args_3_0= ruleAnd ) ) )*
            {

            			newCompositeNode(grammarAccess.getOrPlaAccess().getAndPlaParserRuleCall_0());
            		
            pushFollow(FOLLOW_18);
            this_AndPla_0=ruleAndPla();

            state._fsp--;


            			current = this_AndPla_0;
            			afterParserOrEnumRuleCall();
            		
            // InternalFSMDSL.g:821:3: ( () otherlv_2= '+' ( (lv_args_3_0= ruleAnd ) ) )*
            loop22:
            do {
                int alt22=2;
                int LA22_0 = input.LA(1);

                if ( (LA22_0==20) ) {
                    alt22=1;
                }


                switch (alt22) {
            	case 1 :
            	    // InternalFSMDSL.g:822:4: () otherlv_2= '+' ( (lv_args_3_0= ruleAnd ) )
            	    {
            	    // InternalFSMDSL.g:822:4: ()
            	    // InternalFSMDSL.g:823:5: 
            	    {

            	    					current = forceCreateModelElementAndAdd(
            	    						grammarAccess.getOrPlaAccess().getOrExprArgsAction_1_0(),
            	    						current);
            	    				

            	    }

            	    otherlv_2=(Token)match(input,20,FOLLOW_12); 

            	    				newLeafNode(otherlv_2, grammarAccess.getOrPlaAccess().getPlusSignKeyword_1_1());
            	    			
            	    // InternalFSMDSL.g:833:4: ( (lv_args_3_0= ruleAnd ) )
            	    // InternalFSMDSL.g:834:5: (lv_args_3_0= ruleAnd )
            	    {
            	    // InternalFSMDSL.g:834:5: (lv_args_3_0= ruleAnd )
            	    // InternalFSMDSL.g:835:6: lv_args_3_0= ruleAnd
            	    {

            	    						newCompositeNode(grammarAccess.getOrPlaAccess().getArgsAndParserRuleCall_1_2_0());
            	    					
            	    pushFollow(FOLLOW_18);
            	    lv_args_3_0=ruleAnd();

            	    state._fsp--;


            	    						if (current==null) {
            	    							current = createModelElementForParent(grammarAccess.getOrPlaRule());
            	    						}
            	    						add(
            	    							current,
            	    							"args",
            	    							lv_args_3_0,
            	    							"com.cburch.logisim.statemachine.FSMDSL.And");
            	    						afterParserOrEnumRuleCall();
            	    					

            	    }


            	    }


            	    }
            	    break;

            	default :
            	    break loop22;
                }
            } while (true);


            }


            }


            	leaveRule();

        }

            catch (RecognitionException re) {
                recover(input,re);
                appendSkippedTokens();
            }
        finally {
        }
        return current;
    }
    // $ANTLR end "ruleOrPla"


    // $ANTLR start "entryRuleAndPla"
    // InternalFSMDSL.g:857:1: entryRuleAndPla returns [EObject current=null] : iv_ruleAndPla= ruleAndPla EOF ;
    public final EObject entryRuleAndPla() throws RecognitionException {
        EObject current = null;

        EObject iv_ruleAndPla = null;


        try {
            // InternalFSMDSL.g:857:47: (iv_ruleAndPla= ruleAndPla EOF )
            // InternalFSMDSL.g:858:2: iv_ruleAndPla= ruleAndPla EOF
            {
             newCompositeNode(grammarAccess.getAndPlaRule()); 
            pushFollow(FOLLOW_1);
            iv_ruleAndPla=ruleAndPla();

            state._fsp--;

             current =iv_ruleAndPla; 
            match(input,EOF,FOLLOW_2); 

            }

        }

            catch (RecognitionException re) {
                recover(input,re);
                appendSkippedTokens();
            }
        finally {
        }
        return current;
    }
    // $ANTLR end "entryRuleAndPla"


    // $ANTLR start "ruleAndPla"
    // InternalFSMDSL.g:864:1: ruleAndPla returns [EObject current=null] : (this_PrimaryPla_0= rulePrimaryPla ( () otherlv_2= '*' ( (lv_args_3_0= rulePrimaryPla ) ) )* ) ;
    public final EObject ruleAndPla() throws RecognitionException {
        EObject current = null;

        Token otherlv_2=null;
        EObject this_PrimaryPla_0 = null;

        EObject lv_args_3_0 = null;



        	enterRule();

        try {
            // InternalFSMDSL.g:870:2: ( (this_PrimaryPla_0= rulePrimaryPla ( () otherlv_2= '*' ( (lv_args_3_0= rulePrimaryPla ) ) )* ) )
            // InternalFSMDSL.g:871:2: (this_PrimaryPla_0= rulePrimaryPla ( () otherlv_2= '*' ( (lv_args_3_0= rulePrimaryPla ) ) )* )
            {
            // InternalFSMDSL.g:871:2: (this_PrimaryPla_0= rulePrimaryPla ( () otherlv_2= '*' ( (lv_args_3_0= rulePrimaryPla ) ) )* )
            // InternalFSMDSL.g:872:3: this_PrimaryPla_0= rulePrimaryPla ( () otherlv_2= '*' ( (lv_args_3_0= rulePrimaryPla ) ) )*
            {

            			newCompositeNode(grammarAccess.getAndPlaAccess().getPrimaryPlaParserRuleCall_0());
            		
            pushFollow(FOLLOW_19);
            this_PrimaryPla_0=rulePrimaryPla();

            state._fsp--;


            			current = this_PrimaryPla_0;
            			afterParserOrEnumRuleCall();
            		
            // InternalFSMDSL.g:880:3: ( () otherlv_2= '*' ( (lv_args_3_0= rulePrimaryPla ) ) )*
            loop23:
            do {
                int alt23=2;
                int LA23_0 = input.LA(1);

                if ( (LA23_0==21) ) {
                    alt23=1;
                }


                switch (alt23) {
            	case 1 :
            	    // InternalFSMDSL.g:881:4: () otherlv_2= '*' ( (lv_args_3_0= rulePrimaryPla ) )
            	    {
            	    // InternalFSMDSL.g:881:4: ()
            	    // InternalFSMDSL.g:882:5: 
            	    {

            	    					current = forceCreateModelElementAndAdd(
            	    						grammarAccess.getAndPlaAccess().getAndExprArgsAction_1_0(),
            	    						current);
            	    				

            	    }

            	    otherlv_2=(Token)match(input,21,FOLLOW_17); 

            	    				newLeafNode(otherlv_2, grammarAccess.getAndPlaAccess().getAsteriskKeyword_1_1());
            	    			
            	    // InternalFSMDSL.g:892:4: ( (lv_args_3_0= rulePrimaryPla ) )
            	    // InternalFSMDSL.g:893:5: (lv_args_3_0= rulePrimaryPla )
            	    {
            	    // InternalFSMDSL.g:893:5: (lv_args_3_0= rulePrimaryPla )
            	    // InternalFSMDSL.g:894:6: lv_args_3_0= rulePrimaryPla
            	    {

            	    						newCompositeNode(grammarAccess.getAndPlaAccess().getArgsPrimaryPlaParserRuleCall_1_2_0());
            	    					
            	    pushFollow(FOLLOW_19);
            	    lv_args_3_0=rulePrimaryPla();

            	    state._fsp--;


            	    						if (current==null) {
            	    							current = createModelElementForParent(grammarAccess.getAndPlaRule());
            	    						}
            	    						add(
            	    							current,
            	    							"args",
            	    							lv_args_3_0,
            	    							"com.cburch.logisim.statemachine.FSMDSL.PrimaryPla");
            	    						afterParserOrEnumRuleCall();
            	    					

            	    }


            	    }


            	    }
            	    break;

            	default :
            	    break loop23;
                }
            } while (true);


            }


            }


            	leaveRule();

        }

            catch (RecognitionException re) {
                recover(input,re);
                appendSkippedTokens();
            }
        finally {
        }
        return current;
    }
    // $ANTLR end "ruleAndPla"


    // $ANTLR start "entryRulePrimaryPla"
    // InternalFSMDSL.g:916:1: entryRulePrimaryPla returns [EObject current=null] : iv_rulePrimaryPla= rulePrimaryPla EOF ;
    public final EObject entryRulePrimaryPla() throws RecognitionException {
        EObject current = null;

        EObject iv_rulePrimaryPla = null;


        try {
            // InternalFSMDSL.g:916:51: (iv_rulePrimaryPla= rulePrimaryPla EOF )
            // InternalFSMDSL.g:917:2: iv_rulePrimaryPla= rulePrimaryPla EOF
            {
             newCompositeNode(grammarAccess.getPrimaryPlaRule()); 
            pushFollow(FOLLOW_1);
            iv_rulePrimaryPla=rulePrimaryPla();

            state._fsp--;

             current =iv_rulePrimaryPla; 
            match(input,EOF,FOLLOW_2); 

            }

        }

            catch (RecognitionException re) {
                recover(input,re);
                appendSkippedTokens();
            }
        finally {
        }
        return current;
    }
    // $ANTLR end "entryRulePrimaryPla"


    // $ANTLR start "rulePrimaryPla"
    // InternalFSMDSL.g:923:1: rulePrimaryPla returns [EObject current=null] : ( (otherlv_0= '(' this_OrPla_1= ruleOrPla otherlv_2= ')' ) | this_NotPla_3= ruleNotPla | this_PortRefPla_4= rulePortRefPla ) ;
    public final EObject rulePrimaryPla() throws RecognitionException {
        EObject current = null;

        Token otherlv_0=null;
        Token otherlv_2=null;
        EObject this_OrPla_1 = null;

        EObject this_NotPla_3 = null;

        EObject this_PortRefPla_4 = null;



        	enterRule();

        try {
            // InternalFSMDSL.g:929:2: ( ( (otherlv_0= '(' this_OrPla_1= ruleOrPla otherlv_2= ')' ) | this_NotPla_3= ruleNotPla | this_PortRefPla_4= rulePortRefPla ) )
            // InternalFSMDSL.g:930:2: ( (otherlv_0= '(' this_OrPla_1= ruleOrPla otherlv_2= ')' ) | this_NotPla_3= ruleNotPla | this_PortRefPla_4= rulePortRefPla )
            {
            // InternalFSMDSL.g:930:2: ( (otherlv_0= '(' this_OrPla_1= ruleOrPla otherlv_2= ')' ) | this_NotPla_3= ruleNotPla | this_PortRefPla_4= rulePortRefPla )
            int alt24=3;
            switch ( input.LA(1) ) {
            case 22:
                {
                alt24=1;
                }
                break;
            case 24:
                {
                alt24=2;
                }
                break;
            case RULE_ID:
                {
                alt24=3;
                }
                break;
            default:
                NoViableAltException nvae =
                    new NoViableAltException("", 24, 0, input);

                throw nvae;
            }

            switch (alt24) {
                case 1 :
                    // InternalFSMDSL.g:931:3: (otherlv_0= '(' this_OrPla_1= ruleOrPla otherlv_2= ')' )
                    {
                    // InternalFSMDSL.g:931:3: (otherlv_0= '(' this_OrPla_1= ruleOrPla otherlv_2= ')' )
                    // InternalFSMDSL.g:932:4: otherlv_0= '(' this_OrPla_1= ruleOrPla otherlv_2= ')'
                    {
                    otherlv_0=(Token)match(input,22,FOLLOW_17); 

                    				newLeafNode(otherlv_0, grammarAccess.getPrimaryPlaAccess().getLeftParenthesisKeyword_0_0());
                    			

                    				newCompositeNode(grammarAccess.getPrimaryPlaAccess().getOrPlaParserRuleCall_0_1());
                    			
                    pushFollow(FOLLOW_20);
                    this_OrPla_1=ruleOrPla();

                    state._fsp--;


                    				current = this_OrPla_1;
                    				afterParserOrEnumRuleCall();
                    			
                    otherlv_2=(Token)match(input,23,FOLLOW_2); 

                    				newLeafNode(otherlv_2, grammarAccess.getPrimaryPlaAccess().getRightParenthesisKeyword_0_2());
                    			

                    }


                    }
                    break;
                case 2 :
                    // InternalFSMDSL.g:950:3: this_NotPla_3= ruleNotPla
                    {

                    			newCompositeNode(grammarAccess.getPrimaryPlaAccess().getNotPlaParserRuleCall_1());
                    		
                    pushFollow(FOLLOW_2);
                    this_NotPla_3=ruleNotPla();

                    state._fsp--;


                    			current = this_NotPla_3;
                    			afterParserOrEnumRuleCall();
                    		

                    }
                    break;
                case 3 :
                    // InternalFSMDSL.g:959:3: this_PortRefPla_4= rulePortRefPla
                    {

                    			newCompositeNode(grammarAccess.getPrimaryPlaAccess().getPortRefPlaParserRuleCall_2());
                    		
                    pushFollow(FOLLOW_2);
                    this_PortRefPla_4=rulePortRefPla();

                    state._fsp--;


                    			current = this_PortRefPla_4;
                    			afterParserOrEnumRuleCall();
                    		

                    }
                    break;

            }


            }


            	leaveRule();

        }

            catch (RecognitionException re) {
                recover(input,re);
                appendSkippedTokens();
            }
        finally {
        }
        return current;
    }
    // $ANTLR end "rulePrimaryPla"


    // $ANTLR start "entryRuleNotPla"
    // InternalFSMDSL.g:971:1: entryRuleNotPla returns [EObject current=null] : iv_ruleNotPla= ruleNotPla EOF ;
    public final EObject entryRuleNotPla() throws RecognitionException {
        EObject current = null;

        EObject iv_ruleNotPla = null;


        try {
            // InternalFSMDSL.g:971:47: (iv_ruleNotPla= ruleNotPla EOF )
            // InternalFSMDSL.g:972:2: iv_ruleNotPla= ruleNotPla EOF
            {
             newCompositeNode(grammarAccess.getNotPlaRule()); 
            pushFollow(FOLLOW_1);
            iv_ruleNotPla=ruleNotPla();

            state._fsp--;

             current =iv_ruleNotPla; 
            match(input,EOF,FOLLOW_2); 

            }

        }

            catch (RecognitionException re) {
                recover(input,re);
                appendSkippedTokens();
            }
        finally {
        }
        return current;
    }
    // $ANTLR end "entryRuleNotPla"


    // $ANTLR start "ruleNotPla"
    // InternalFSMDSL.g:978:1: ruleNotPla returns [EObject current=null] : ( () otherlv_1= '!' ( (lv_args_2_0= rulePrimaryPla ) ) ) ;
    public final EObject ruleNotPla() throws RecognitionException {
        EObject current = null;

        Token otherlv_1=null;
        EObject lv_args_2_0 = null;



        	enterRule();

        try {
            // InternalFSMDSL.g:984:2: ( ( () otherlv_1= '!' ( (lv_args_2_0= rulePrimaryPla ) ) ) )
            // InternalFSMDSL.g:985:2: ( () otherlv_1= '!' ( (lv_args_2_0= rulePrimaryPla ) ) )
            {
            // InternalFSMDSL.g:985:2: ( () otherlv_1= '!' ( (lv_args_2_0= rulePrimaryPla ) ) )
            // InternalFSMDSL.g:986:3: () otherlv_1= '!' ( (lv_args_2_0= rulePrimaryPla ) )
            {
            // InternalFSMDSL.g:986:3: ()
            // InternalFSMDSL.g:987:4: 
            {

            				current = forceCreateModelElement(
            					grammarAccess.getNotPlaAccess().getNotExprAction_0(),
            					current);
            			

            }

            otherlv_1=(Token)match(input,24,FOLLOW_17); 

            			newLeafNode(otherlv_1, grammarAccess.getNotPlaAccess().getExclamationMarkKeyword_1());
            		
            // InternalFSMDSL.g:997:3: ( (lv_args_2_0= rulePrimaryPla ) )
            // InternalFSMDSL.g:998:4: (lv_args_2_0= rulePrimaryPla )
            {
            // InternalFSMDSL.g:998:4: (lv_args_2_0= rulePrimaryPla )
            // InternalFSMDSL.g:999:5: lv_args_2_0= rulePrimaryPla
            {

            					newCompositeNode(grammarAccess.getNotPlaAccess().getArgsPrimaryPlaParserRuleCall_2_0());
            				
            pushFollow(FOLLOW_2);
            lv_args_2_0=rulePrimaryPla();

            state._fsp--;


            					if (current==null) {
            						current = createModelElementForParent(grammarAccess.getNotPlaRule());
            					}
            					add(
            						current,
            						"args",
            						lv_args_2_0,
            						"com.cburch.logisim.statemachine.FSMDSL.PrimaryPla");
            					afterParserOrEnumRuleCall();
            				

            }


            }


            }


            }


            	leaveRule();

        }

            catch (RecognitionException re) {
                recover(input,re);
                appendSkippedTokens();
            }
        finally {
        }
        return current;
    }
    // $ANTLR end "ruleNotPla"


    // $ANTLR start "entryRulePortRefPla"
    // InternalFSMDSL.g:1020:1: entryRulePortRefPla returns [EObject current=null] : iv_rulePortRefPla= rulePortRefPla EOF ;
    public final EObject entryRulePortRefPla() throws RecognitionException {
        EObject current = null;

        EObject iv_rulePortRefPla = null;


        try {
            // InternalFSMDSL.g:1020:51: (iv_rulePortRefPla= rulePortRefPla EOF )
            // InternalFSMDSL.g:1021:2: iv_rulePortRefPla= rulePortRefPla EOF
            {
             newCompositeNode(grammarAccess.getPortRefPlaRule()); 
            pushFollow(FOLLOW_1);
            iv_rulePortRefPla=rulePortRefPla();

            state._fsp--;

             current =iv_rulePortRefPla; 
            match(input,EOF,FOLLOW_2); 

            }

        }

            catch (RecognitionException re) {
                recover(input,re);
                appendSkippedTokens();
            }
        finally {
        }
        return current;
    }
    // $ANTLR end "entryRulePortRefPla"


    // $ANTLR start "rulePortRefPla"
    // InternalFSMDSL.g:1027:1: rulePortRefPla returns [EObject current=null] : ( () ( (otherlv_1= RULE_ID ) ) ) ;
    public final EObject rulePortRefPla() throws RecognitionException {
        EObject current = null;

        Token otherlv_1=null;


        	enterRule();

        try {
            // InternalFSMDSL.g:1033:2: ( ( () ( (otherlv_1= RULE_ID ) ) ) )
            // InternalFSMDSL.g:1034:2: ( () ( (otherlv_1= RULE_ID ) ) )
            {
            // InternalFSMDSL.g:1034:2: ( () ( (otherlv_1= RULE_ID ) ) )
            // InternalFSMDSL.g:1035:3: () ( (otherlv_1= RULE_ID ) )
            {
            // InternalFSMDSL.g:1035:3: ()
            // InternalFSMDSL.g:1036:4: 
            {

            				current = forceCreateModelElement(
            					grammarAccess.getPortRefPlaAccess().getPortRefAction_0(),
            					current);
            			

            }

            // InternalFSMDSL.g:1042:3: ( (otherlv_1= RULE_ID ) )
            // InternalFSMDSL.g:1043:4: (otherlv_1= RULE_ID )
            {
            // InternalFSMDSL.g:1043:4: (otherlv_1= RULE_ID )
            // InternalFSMDSL.g:1044:5: otherlv_1= RULE_ID
            {

            					if (current==null) {
            						current = createModelElement(grammarAccess.getPortRefPlaRule());
            					}
            				
            otherlv_1=(Token)match(input,RULE_ID,FOLLOW_2); 

            					newLeafNode(otherlv_1, grammarAccess.getPortRefPlaAccess().getPortPortCrossReference_1_0());
            				

            }


            }


            }


            }


            	leaveRule();

        }

            catch (RecognitionException re) {
                recover(input,re);
                appendSkippedTokens();
            }
        finally {
        }
        return current;
    }
    // $ANTLR end "rulePortRefPla"


    // $ANTLR start "entryRuleFSM"
    // InternalFSMDSL.g:1059:1: entryRuleFSM returns [EObject current=null] : iv_ruleFSM= ruleFSM EOF ;
    public final EObject entryRuleFSM() throws RecognitionException {
        EObject current = null;

        EObject iv_ruleFSM = null;


        try {
            // InternalFSMDSL.g:1059:44: (iv_ruleFSM= ruleFSM EOF )
            // InternalFSMDSL.g:1060:2: iv_ruleFSM= ruleFSM EOF
            {
             newCompositeNode(grammarAccess.getFSMRule()); 
            pushFollow(FOLLOW_1);
            iv_ruleFSM=ruleFSM();

            state._fsp--;

             current =iv_ruleFSM; 
            match(input,EOF,FOLLOW_2); 

            }

        }

            catch (RecognitionException re) {
                recover(input,re);
                appendSkippedTokens();
            }
        finally {
        }
        return current;
    }
    // $ANTLR end "entryRuleFSM"


    // $ANTLR start "ruleFSM"
    // InternalFSMDSL.g:1066:1: ruleFSM returns [EObject current=null] : ( () (otherlv_1= 'fsm' | otherlv_2= 'state_machine' ) ( (lv_name_3_0= RULE_ID ) ) ( (lv_layout_4_0= ruleLayoutInfo ) ) otherlv_5= '{' ( ( (lv_constants_6_0= ruleConstantDef ) ) otherlv_7= ';' )* ( (otherlv_8= 'in' ( (lv_in_9_0= ruleInputPort ) )+ ) | (otherlv_10= 'out' ( (lv_out_11_0= ruleOutputPort ) )+ ) )+ (otherlv_12= 'codeWidth' otherlv_13= '=' ( (lv_width_14_0= RULE_INT ) ) otherlv_15= ';' ) (otherlv_16= 'reset' otherlv_17= '=' ( (otherlv_18= RULE_ID ) ) otherlv_19= ';' ) ( ( (lv_states_20_1= ruleLongState | lv_states_20_2= ruleShortState ) ) )* otherlv_21= '}' ) ;
    public final EObject ruleFSM() throws RecognitionException {
        EObject current = null;

        Token otherlv_1=null;
        Token otherlv_2=null;
        Token lv_name_3_0=null;
        Token otherlv_5=null;
        Token otherlv_7=null;
        Token otherlv_8=null;
        Token otherlv_10=null;
        Token otherlv_12=null;
        Token otherlv_13=null;
        Token lv_width_14_0=null;
        Token otherlv_15=null;
        Token otherlv_16=null;
        Token otherlv_17=null;
        Token otherlv_18=null;
        Token otherlv_19=null;
        Token otherlv_21=null;
        EObject lv_layout_4_0 = null;

        EObject lv_constants_6_0 = null;

        EObject lv_in_9_0 = null;

        EObject lv_out_11_0 = null;

        EObject lv_states_20_1 = null;

        EObject lv_states_20_2 = null;



        	enterRule();

        try {
            // InternalFSMDSL.g:1072:2: ( ( () (otherlv_1= 'fsm' | otherlv_2= 'state_machine' ) ( (lv_name_3_0= RULE_ID ) ) ( (lv_layout_4_0= ruleLayoutInfo ) ) otherlv_5= '{' ( ( (lv_constants_6_0= ruleConstantDef ) ) otherlv_7= ';' )* ( (otherlv_8= 'in' ( (lv_in_9_0= ruleInputPort ) )+ ) | (otherlv_10= 'out' ( (lv_out_11_0= ruleOutputPort ) )+ ) )+ (otherlv_12= 'codeWidth' otherlv_13= '=' ( (lv_width_14_0= RULE_INT ) ) otherlv_15= ';' ) (otherlv_16= 'reset' otherlv_17= '=' ( (otherlv_18= RULE_ID ) ) otherlv_19= ';' ) ( ( (lv_states_20_1= ruleLongState | lv_states_20_2= ruleShortState ) ) )* otherlv_21= '}' ) )
            // InternalFSMDSL.g:1073:2: ( () (otherlv_1= 'fsm' | otherlv_2= 'state_machine' ) ( (lv_name_3_0= RULE_ID ) ) ( (lv_layout_4_0= ruleLayoutInfo ) ) otherlv_5= '{' ( ( (lv_constants_6_0= ruleConstantDef ) ) otherlv_7= ';' )* ( (otherlv_8= 'in' ( (lv_in_9_0= ruleInputPort ) )+ ) | (otherlv_10= 'out' ( (lv_out_11_0= ruleOutputPort ) )+ ) )+ (otherlv_12= 'codeWidth' otherlv_13= '=' ( (lv_width_14_0= RULE_INT ) ) otherlv_15= ';' ) (otherlv_16= 'reset' otherlv_17= '=' ( (otherlv_18= RULE_ID ) ) otherlv_19= ';' ) ( ( (lv_states_20_1= ruleLongState | lv_states_20_2= ruleShortState ) ) )* otherlv_21= '}' )
            {
            // InternalFSMDSL.g:1073:2: ( () (otherlv_1= 'fsm' | otherlv_2= 'state_machine' ) ( (lv_name_3_0= RULE_ID ) ) ( (lv_layout_4_0= ruleLayoutInfo ) ) otherlv_5= '{' ( ( (lv_constants_6_0= ruleConstantDef ) ) otherlv_7= ';' )* ( (otherlv_8= 'in' ( (lv_in_9_0= ruleInputPort ) )+ ) | (otherlv_10= 'out' ( (lv_out_11_0= ruleOutputPort ) )+ ) )+ (otherlv_12= 'codeWidth' otherlv_13= '=' ( (lv_width_14_0= RULE_INT ) ) otherlv_15= ';' ) (otherlv_16= 'reset' otherlv_17= '=' ( (otherlv_18= RULE_ID ) ) otherlv_19= ';' ) ( ( (lv_states_20_1= ruleLongState | lv_states_20_2= ruleShortState ) ) )* otherlv_21= '}' )
            // InternalFSMDSL.g:1074:3: () (otherlv_1= 'fsm' | otherlv_2= 'state_machine' ) ( (lv_name_3_0= RULE_ID ) ) ( (lv_layout_4_0= ruleLayoutInfo ) ) otherlv_5= '{' ( ( (lv_constants_6_0= ruleConstantDef ) ) otherlv_7= ';' )* ( (otherlv_8= 'in' ( (lv_in_9_0= ruleInputPort ) )+ ) | (otherlv_10= 'out' ( (lv_out_11_0= ruleOutputPort ) )+ ) )+ (otherlv_12= 'codeWidth' otherlv_13= '=' ( (lv_width_14_0= RULE_INT ) ) otherlv_15= ';' ) (otherlv_16= 'reset' otherlv_17= '=' ( (otherlv_18= RULE_ID ) ) otherlv_19= ';' ) ( ( (lv_states_20_1= ruleLongState | lv_states_20_2= ruleShortState ) ) )* otherlv_21= '}'
            {
            // InternalFSMDSL.g:1074:3: ()
            // InternalFSMDSL.g:1075:4: 
            {

            				current = forceCreateModelElement(
            					grammarAccess.getFSMAccess().getFSMAction_0(),
            					current);
            			

            }

            // InternalFSMDSL.g:1081:3: (otherlv_1= 'fsm' | otherlv_2= 'state_machine' )
            int alt25=2;
            int LA25_0 = input.LA(1);

            if ( (LA25_0==25) ) {
                alt25=1;
            }
            else if ( (LA25_0==26) ) {
                alt25=2;
            }
            else {
                NoViableAltException nvae =
                    new NoViableAltException("", 25, 0, input);

                throw nvae;
            }
            switch (alt25) {
                case 1 :
                    // InternalFSMDSL.g:1082:4: otherlv_1= 'fsm'
                    {
                    otherlv_1=(Token)match(input,25,FOLLOW_8); 

                    				newLeafNode(otherlv_1, grammarAccess.getFSMAccess().getFsmKeyword_1_0());
                    			

                    }
                    break;
                case 2 :
                    // InternalFSMDSL.g:1087:4: otherlv_2= 'state_machine'
                    {
                    otherlv_2=(Token)match(input,26,FOLLOW_8); 

                    				newLeafNode(otherlv_2, grammarAccess.getFSMAccess().getState_machineKeyword_1_1());
                    			

                    }
                    break;

            }

            // InternalFSMDSL.g:1092:3: ( (lv_name_3_0= RULE_ID ) )
            // InternalFSMDSL.g:1093:4: (lv_name_3_0= RULE_ID )
            {
            // InternalFSMDSL.g:1093:4: (lv_name_3_0= RULE_ID )
            // InternalFSMDSL.g:1094:5: lv_name_3_0= RULE_ID
            {
            lv_name_3_0=(Token)match(input,RULE_ID,FOLLOW_21); 

            					newLeafNode(lv_name_3_0, grammarAccess.getFSMAccess().getNameIDTerminalRuleCall_2_0());
            				

            					if (current==null) {
            						current = createModelElement(grammarAccess.getFSMRule());
            					}
            					setWithLastConsumed(
            						current,
            						"name",
            						lv_name_3_0,
            						"org.eclipse.xtext.common.Terminals.ID");
            				

            }


            }

            // InternalFSMDSL.g:1110:3: ( (lv_layout_4_0= ruleLayoutInfo ) )
            // InternalFSMDSL.g:1111:4: (lv_layout_4_0= ruleLayoutInfo )
            {
            // InternalFSMDSL.g:1111:4: (lv_layout_4_0= ruleLayoutInfo )
            // InternalFSMDSL.g:1112:5: lv_layout_4_0= ruleLayoutInfo
            {

            					newCompositeNode(grammarAccess.getFSMAccess().getLayoutLayoutInfoParserRuleCall_3_0());
            				
            pushFollow(FOLLOW_22);
            lv_layout_4_0=ruleLayoutInfo();

            state._fsp--;


            					if (current==null) {
            						current = createModelElementForParent(grammarAccess.getFSMRule());
            					}
            					set(
            						current,
            						"layout",
            						lv_layout_4_0,
            						"com.cburch.logisim.statemachine.FSMDSL.LayoutInfo");
            					afterParserOrEnumRuleCall();
            				

            }


            }

            otherlv_5=(Token)match(input,27,FOLLOW_23); 

            			newLeafNode(otherlv_5, grammarAccess.getFSMAccess().getLeftCurlyBracketKeyword_4());
            		
            // InternalFSMDSL.g:1133:3: ( ( (lv_constants_6_0= ruleConstantDef ) ) otherlv_7= ';' )*
            loop26:
            do {
                int alt26=2;
                int LA26_0 = input.LA(1);

                if ( (LA26_0==48) ) {
                    alt26=1;
                }


                switch (alt26) {
            	case 1 :
            	    // InternalFSMDSL.g:1134:4: ( (lv_constants_6_0= ruleConstantDef ) ) otherlv_7= ';'
            	    {
            	    // InternalFSMDSL.g:1134:4: ( (lv_constants_6_0= ruleConstantDef ) )
            	    // InternalFSMDSL.g:1135:5: (lv_constants_6_0= ruleConstantDef )
            	    {
            	    // InternalFSMDSL.g:1135:5: (lv_constants_6_0= ruleConstantDef )
            	    // InternalFSMDSL.g:1136:6: lv_constants_6_0= ruleConstantDef
            	    {

            	    						newCompositeNode(grammarAccess.getFSMAccess().getConstantsConstantDefParserRuleCall_5_0_0());
            	    					
            	    pushFollow(FOLLOW_11);
            	    lv_constants_6_0=ruleConstantDef();

            	    state._fsp--;


            	    						if (current==null) {
            	    							current = createModelElementForParent(grammarAccess.getFSMRule());
            	    						}
            	    						add(
            	    							current,
            	    							"constants",
            	    							lv_constants_6_0,
            	    							"com.cburch.logisim.statemachine.FSMDSL.ConstantDef");
            	    						afterParserOrEnumRuleCall();
            	    					

            	    }


            	    }

            	    otherlv_7=(Token)match(input,16,FOLLOW_23); 

            	    				newLeafNode(otherlv_7, grammarAccess.getFSMAccess().getSemicolonKeyword_5_1());
            	    			

            	    }
            	    break;

            	default :
            	    break loop26;
                }
            } while (true);

            // InternalFSMDSL.g:1158:3: ( (otherlv_8= 'in' ( (lv_in_9_0= ruleInputPort ) )+ ) | (otherlv_10= 'out' ( (lv_out_11_0= ruleOutputPort ) )+ ) )+
            int cnt29=0;
            loop29:
            do {
                int alt29=3;
                int LA29_0 = input.LA(1);

                if ( (LA29_0==28) ) {
                    alt29=1;
                }
                else if ( (LA29_0==29) ) {
                    alt29=2;
                }


                switch (alt29) {
            	case 1 :
            	    // InternalFSMDSL.g:1159:4: (otherlv_8= 'in' ( (lv_in_9_0= ruleInputPort ) )+ )
            	    {
            	    // InternalFSMDSL.g:1159:4: (otherlv_8= 'in' ( (lv_in_9_0= ruleInputPort ) )+ )
            	    // InternalFSMDSL.g:1160:5: otherlv_8= 'in' ( (lv_in_9_0= ruleInputPort ) )+
            	    {
            	    otherlv_8=(Token)match(input,28,FOLLOW_8); 

            	    					newLeafNode(otherlv_8, grammarAccess.getFSMAccess().getInKeyword_6_0_0());
            	    				
            	    // InternalFSMDSL.g:1164:5: ( (lv_in_9_0= ruleInputPort ) )+
            	    int cnt27=0;
            	    loop27:
            	    do {
            	        int alt27=2;
            	        int LA27_0 = input.LA(1);

            	        if ( (LA27_0==RULE_ID) ) {
            	            alt27=1;
            	        }


            	        switch (alt27) {
            	    	case 1 :
            	    	    // InternalFSMDSL.g:1165:6: (lv_in_9_0= ruleInputPort )
            	    	    {
            	    	    // InternalFSMDSL.g:1165:6: (lv_in_9_0= ruleInputPort )
            	    	    // InternalFSMDSL.g:1166:7: lv_in_9_0= ruleInputPort
            	    	    {

            	    	    							newCompositeNode(grammarAccess.getFSMAccess().getInInputPortParserRuleCall_6_0_1_0());
            	    	    						
            	    	    pushFollow(FOLLOW_24);
            	    	    lv_in_9_0=ruleInputPort();

            	    	    state._fsp--;


            	    	    							if (current==null) {
            	    	    								current = createModelElementForParent(grammarAccess.getFSMRule());
            	    	    							}
            	    	    							add(
            	    	    								current,
            	    	    								"in",
            	    	    								lv_in_9_0,
            	    	    								"com.cburch.logisim.statemachine.FSMDSL.InputPort");
            	    	    							afterParserOrEnumRuleCall();
            	    	    						

            	    	    }


            	    	    }
            	    	    break;

            	    	default :
            	    	    if ( cnt27 >= 1 ) break loop27;
            	                EarlyExitException eee =
            	                    new EarlyExitException(27, input);
            	                throw eee;
            	        }
            	        cnt27++;
            	    } while (true);


            	    }


            	    }
            	    break;
            	case 2 :
            	    // InternalFSMDSL.g:1185:4: (otherlv_10= 'out' ( (lv_out_11_0= ruleOutputPort ) )+ )
            	    {
            	    // InternalFSMDSL.g:1185:4: (otherlv_10= 'out' ( (lv_out_11_0= ruleOutputPort ) )+ )
            	    // InternalFSMDSL.g:1186:5: otherlv_10= 'out' ( (lv_out_11_0= ruleOutputPort ) )+
            	    {
            	    otherlv_10=(Token)match(input,29,FOLLOW_8); 

            	    					newLeafNode(otherlv_10, grammarAccess.getFSMAccess().getOutKeyword_6_1_0());
            	    				
            	    // InternalFSMDSL.g:1190:5: ( (lv_out_11_0= ruleOutputPort ) )+
            	    int cnt28=0;
            	    loop28:
            	    do {
            	        int alt28=2;
            	        int LA28_0 = input.LA(1);

            	        if ( (LA28_0==RULE_ID) ) {
            	            alt28=1;
            	        }


            	        switch (alt28) {
            	    	case 1 :
            	    	    // InternalFSMDSL.g:1191:6: (lv_out_11_0= ruleOutputPort )
            	    	    {
            	    	    // InternalFSMDSL.g:1191:6: (lv_out_11_0= ruleOutputPort )
            	    	    // InternalFSMDSL.g:1192:7: lv_out_11_0= ruleOutputPort
            	    	    {

            	    	    							newCompositeNode(grammarAccess.getFSMAccess().getOutOutputPortParserRuleCall_6_1_1_0());
            	    	    						
            	    	    pushFollow(FOLLOW_24);
            	    	    lv_out_11_0=ruleOutputPort();

            	    	    state._fsp--;


            	    	    							if (current==null) {
            	    	    								current = createModelElementForParent(grammarAccess.getFSMRule());
            	    	    							}
            	    	    							add(
            	    	    								current,
            	    	    								"out",
            	    	    								lv_out_11_0,
            	    	    								"com.cburch.logisim.statemachine.FSMDSL.OutputPort");
            	    	    							afterParserOrEnumRuleCall();
            	    	    						

            	    	    }


            	    	    }
            	    	    break;

            	    	default :
            	    	    if ( cnt28 >= 1 ) break loop28;
            	                EarlyExitException eee =
            	                    new EarlyExitException(28, input);
            	                throw eee;
            	        }
            	        cnt28++;
            	    } while (true);


            	    }


            	    }
            	    break;

            	default :
            	    if ( cnt29 >= 1 ) break loop29;
                        EarlyExitException eee =
                            new EarlyExitException(29, input);
                        throw eee;
                }
                cnt29++;
            } while (true);

            // InternalFSMDSL.g:1211:3: (otherlv_12= 'codeWidth' otherlv_13= '=' ( (lv_width_14_0= RULE_INT ) ) otherlv_15= ';' )
            // InternalFSMDSL.g:1212:4: otherlv_12= 'codeWidth' otherlv_13= '=' ( (lv_width_14_0= RULE_INT ) ) otherlv_15= ';'
            {
            otherlv_12=(Token)match(input,30,FOLLOW_13); 

            				newLeafNode(otherlv_12, grammarAccess.getFSMAccess().getCodeWidthKeyword_7_0());
            			
            otherlv_13=(Token)match(input,18,FOLLOW_25); 

            				newLeafNode(otherlv_13, grammarAccess.getFSMAccess().getEqualsSignKeyword_7_1());
            			
            // InternalFSMDSL.g:1220:4: ( (lv_width_14_0= RULE_INT ) )
            // InternalFSMDSL.g:1221:5: (lv_width_14_0= RULE_INT )
            {
            // InternalFSMDSL.g:1221:5: (lv_width_14_0= RULE_INT )
            // InternalFSMDSL.g:1222:6: lv_width_14_0= RULE_INT
            {
            lv_width_14_0=(Token)match(input,RULE_INT,FOLLOW_11); 

            						newLeafNode(lv_width_14_0, grammarAccess.getFSMAccess().getWidthINTTerminalRuleCall_7_2_0());
            					

            						if (current==null) {
            							current = createModelElement(grammarAccess.getFSMRule());
            						}
            						setWithLastConsumed(
            							current,
            							"width",
            							lv_width_14_0,
            							"org.eclipse.xtext.common.Terminals.INT");
            					

            }


            }

            otherlv_15=(Token)match(input,16,FOLLOW_26); 

            				newLeafNode(otherlv_15, grammarAccess.getFSMAccess().getSemicolonKeyword_7_3());
            			

            }

            // InternalFSMDSL.g:1243:3: (otherlv_16= 'reset' otherlv_17= '=' ( (otherlv_18= RULE_ID ) ) otherlv_19= ';' )
            // InternalFSMDSL.g:1244:4: otherlv_16= 'reset' otherlv_17= '=' ( (otherlv_18= RULE_ID ) ) otherlv_19= ';'
            {
            otherlv_16=(Token)match(input,31,FOLLOW_13); 

            				newLeafNode(otherlv_16, grammarAccess.getFSMAccess().getResetKeyword_8_0());
            			
            otherlv_17=(Token)match(input,18,FOLLOW_8); 

            				newLeafNode(otherlv_17, grammarAccess.getFSMAccess().getEqualsSignKeyword_8_1());
            			
            // InternalFSMDSL.g:1252:4: ( (otherlv_18= RULE_ID ) )
            // InternalFSMDSL.g:1253:5: (otherlv_18= RULE_ID )
            {
            // InternalFSMDSL.g:1253:5: (otherlv_18= RULE_ID )
            // InternalFSMDSL.g:1254:6: otherlv_18= RULE_ID
            {

            						if (current==null) {
            							current = createModelElement(grammarAccess.getFSMRule());
            						}
            					
            otherlv_18=(Token)match(input,RULE_ID,FOLLOW_11); 

            						newLeafNode(otherlv_18, grammarAccess.getFSMAccess().getStartStateCrossReference_8_2_0());
            					

            }


            }

            otherlv_19=(Token)match(input,16,FOLLOW_27); 

            				newLeafNode(otherlv_19, grammarAccess.getFSMAccess().getSemicolonKeyword_8_3());
            			

            }

            // InternalFSMDSL.g:1270:3: ( ( (lv_states_20_1= ruleLongState | lv_states_20_2= ruleShortState ) ) )*
            loop31:
            do {
                int alt31=2;
                int LA31_0 = input.LA(1);

                if ( (LA31_0==35) ) {
                    alt31=1;
                }


                switch (alt31) {
            	case 1 :
            	    // InternalFSMDSL.g:1271:4: ( (lv_states_20_1= ruleLongState | lv_states_20_2= ruleShortState ) )
            	    {
            	    // InternalFSMDSL.g:1271:4: ( (lv_states_20_1= ruleLongState | lv_states_20_2= ruleShortState ) )
            	    // InternalFSMDSL.g:1272:5: (lv_states_20_1= ruleLongState | lv_states_20_2= ruleShortState )
            	    {
            	    // InternalFSMDSL.g:1272:5: (lv_states_20_1= ruleLongState | lv_states_20_2= ruleShortState )
            	    int alt30=2;
            	    int LA30_0 = input.LA(1);

            	    if ( (LA30_0==35) ) {
            	        int LA30_1 = input.LA(2);

            	        if ( (LA30_1==RULE_ID) ) {
            	            int LA30_2 = input.LA(3);

            	            if ( (LA30_2==18||LA30_2==27||LA30_2==34) ) {
            	                alt30=1;
            	            }
            	            else if ( (LA30_2==13||LA30_2==37) ) {
            	                alt30=2;
            	            }
            	            else {
            	                NoViableAltException nvae =
            	                    new NoViableAltException("", 30, 2, input);

            	                throw nvae;
            	            }
            	        }
            	        else {
            	            NoViableAltException nvae =
            	                new NoViableAltException("", 30, 1, input);

            	            throw nvae;
            	        }
            	    }
            	    else {
            	        NoViableAltException nvae =
            	            new NoViableAltException("", 30, 0, input);

            	        throw nvae;
            	    }
            	    switch (alt30) {
            	        case 1 :
            	            // InternalFSMDSL.g:1273:6: lv_states_20_1= ruleLongState
            	            {

            	            						newCompositeNode(grammarAccess.getFSMAccess().getStatesLongStateParserRuleCall_9_0_0());
            	            					
            	            pushFollow(FOLLOW_27);
            	            lv_states_20_1=ruleLongState();

            	            state._fsp--;


            	            						if (current==null) {
            	            							current = createModelElementForParent(grammarAccess.getFSMRule());
            	            						}
            	            						add(
            	            							current,
            	            							"states",
            	            							lv_states_20_1,
            	            							"com.cburch.logisim.statemachine.FSMDSL.LongState");
            	            						afterParserOrEnumRuleCall();
            	            					

            	            }
            	            break;
            	        case 2 :
            	            // InternalFSMDSL.g:1289:6: lv_states_20_2= ruleShortState
            	            {

            	            						newCompositeNode(grammarAccess.getFSMAccess().getStatesShortStateParserRuleCall_9_0_1());
            	            					
            	            pushFollow(FOLLOW_27);
            	            lv_states_20_2=ruleShortState();

            	            state._fsp--;


            	            						if (current==null) {
            	            							current = createModelElementForParent(grammarAccess.getFSMRule());
            	            						}
            	            						add(
            	            							current,
            	            							"states",
            	            							lv_states_20_2,
            	            							"com.cburch.logisim.statemachine.FSMDSL.ShortState");
            	            						afterParserOrEnumRuleCall();
            	            					

            	            }
            	            break;

            	    }


            	    }


            	    }
            	    break;

            	default :
            	    break loop31;
                }
            } while (true);

            otherlv_21=(Token)match(input,32,FOLLOW_2); 

            			newLeafNode(otherlv_21, grammarAccess.getFSMAccess().getRightCurlyBracketKeyword_10());
            		

            }


            }


            	leaveRule();

        }

            catch (RecognitionException re) {
                recover(input,re);
                appendSkippedTokens();
            }
        finally {
        }
        return current;
    }
    // $ANTLR end "ruleFSM"


    // $ANTLR start "entryRuleShortInputPort"
    // InternalFSMDSL.g:1315:1: entryRuleShortInputPort returns [EObject current=null] : iv_ruleShortInputPort= ruleShortInputPort EOF ;
    public final EObject entryRuleShortInputPort() throws RecognitionException {
        EObject current = null;

        EObject iv_ruleShortInputPort = null;


        try {
            // InternalFSMDSL.g:1315:55: (iv_ruleShortInputPort= ruleShortInputPort EOF )
            // InternalFSMDSL.g:1316:2: iv_ruleShortInputPort= ruleShortInputPort EOF
            {
             newCompositeNode(grammarAccess.getShortInputPortRule()); 
            pushFollow(FOLLOW_1);
            iv_ruleShortInputPort=ruleShortInputPort();

            state._fsp--;

             current =iv_ruleShortInputPort; 
            match(input,EOF,FOLLOW_2); 

            }

        }

            catch (RecognitionException re) {
                recover(input,re);
                appendSkippedTokens();
            }
        finally {
        }
        return current;
    }
    // $ANTLR end "entryRuleShortInputPort"


    // $ANTLR start "ruleShortInputPort"
    // InternalFSMDSL.g:1322:1: ruleShortInputPort returns [EObject current=null] : ( () ( (lv_name_1_0= RULE_ID ) ) (otherlv_2= '[' ( (lv_width_3_0= RULE_INT ) ) otherlv_4= ']' )? ) ;
    public final EObject ruleShortInputPort() throws RecognitionException {
        EObject current = null;

        Token lv_name_1_0=null;
        Token otherlv_2=null;
        Token lv_width_3_0=null;
        Token otherlv_4=null;


        	enterRule();

        try {
            // InternalFSMDSL.g:1328:2: ( ( () ( (lv_name_1_0= RULE_ID ) ) (otherlv_2= '[' ( (lv_width_3_0= RULE_INT ) ) otherlv_4= ']' )? ) )
            // InternalFSMDSL.g:1329:2: ( () ( (lv_name_1_0= RULE_ID ) ) (otherlv_2= '[' ( (lv_width_3_0= RULE_INT ) ) otherlv_4= ']' )? )
            {
            // InternalFSMDSL.g:1329:2: ( () ( (lv_name_1_0= RULE_ID ) ) (otherlv_2= '[' ( (lv_width_3_0= RULE_INT ) ) otherlv_4= ']' )? )
            // InternalFSMDSL.g:1330:3: () ( (lv_name_1_0= RULE_ID ) ) (otherlv_2= '[' ( (lv_width_3_0= RULE_INT ) ) otherlv_4= ']' )?
            {
            // InternalFSMDSL.g:1330:3: ()
            // InternalFSMDSL.g:1331:4: 
            {

            				current = forceCreateModelElement(
            					grammarAccess.getShortInputPortAccess().getInputPortAction_0(),
            					current);
            			

            }

            // InternalFSMDSL.g:1337:3: ( (lv_name_1_0= RULE_ID ) )
            // InternalFSMDSL.g:1338:4: (lv_name_1_0= RULE_ID )
            {
            // InternalFSMDSL.g:1338:4: (lv_name_1_0= RULE_ID )
            // InternalFSMDSL.g:1339:5: lv_name_1_0= RULE_ID
            {
            lv_name_1_0=(Token)match(input,RULE_ID,FOLLOW_28); 

            					newLeafNode(lv_name_1_0, grammarAccess.getShortInputPortAccess().getNameIDTerminalRuleCall_1_0());
            				

            					if (current==null) {
            						current = createModelElement(grammarAccess.getShortInputPortRule());
            					}
            					setWithLastConsumed(
            						current,
            						"name",
            						lv_name_1_0,
            						"org.eclipse.xtext.common.Terminals.ID");
            				

            }


            }

            // InternalFSMDSL.g:1355:3: (otherlv_2= '[' ( (lv_width_3_0= RULE_INT ) ) otherlv_4= ']' )?
            int alt32=2;
            int LA32_0 = input.LA(1);

            if ( (LA32_0==13) ) {
                alt32=1;
            }
            switch (alt32) {
                case 1 :
                    // InternalFSMDSL.g:1356:4: otherlv_2= '[' ( (lv_width_3_0= RULE_INT ) ) otherlv_4= ']'
                    {
                    otherlv_2=(Token)match(input,13,FOLLOW_25); 

                    				newLeafNode(otherlv_2, grammarAccess.getShortInputPortAccess().getLeftSquareBracketKeyword_2_0());
                    			
                    // InternalFSMDSL.g:1360:4: ( (lv_width_3_0= RULE_INT ) )
                    // InternalFSMDSL.g:1361:5: (lv_width_3_0= RULE_INT )
                    {
                    // InternalFSMDSL.g:1361:5: (lv_width_3_0= RULE_INT )
                    // InternalFSMDSL.g:1362:6: lv_width_3_0= RULE_INT
                    {
                    lv_width_3_0=(Token)match(input,RULE_INT,FOLLOW_29); 

                    						newLeafNode(lv_width_3_0, grammarAccess.getShortInputPortAccess().getWidthINTTerminalRuleCall_2_1_0());
                    					

                    						if (current==null) {
                    							current = createModelElement(grammarAccess.getShortInputPortRule());
                    						}
                    						setWithLastConsumed(
                    							current,
                    							"width",
                    							lv_width_3_0,
                    							"org.eclipse.xtext.common.Terminals.INT");
                    					

                    }


                    }

                    otherlv_4=(Token)match(input,15,FOLLOW_2); 

                    				newLeafNode(otherlv_4, grammarAccess.getShortInputPortAccess().getRightSquareBracketKeyword_2_2());
                    			

                    }
                    break;

            }


            }


            }


            	leaveRule();

        }

            catch (RecognitionException re) {
                recover(input,re);
                appendSkippedTokens();
            }
        finally {
        }
        return current;
    }
    // $ANTLR end "ruleShortInputPort"


    // $ANTLR start "entryRuleShortOutputPort"
    // InternalFSMDSL.g:1387:1: entryRuleShortOutputPort returns [EObject current=null] : iv_ruleShortOutputPort= ruleShortOutputPort EOF ;
    public final EObject entryRuleShortOutputPort() throws RecognitionException {
        EObject current = null;

        EObject iv_ruleShortOutputPort = null;


        try {
            // InternalFSMDSL.g:1387:56: (iv_ruleShortOutputPort= ruleShortOutputPort EOF )
            // InternalFSMDSL.g:1388:2: iv_ruleShortOutputPort= ruleShortOutputPort EOF
            {
             newCompositeNode(grammarAccess.getShortOutputPortRule()); 
            pushFollow(FOLLOW_1);
            iv_ruleShortOutputPort=ruleShortOutputPort();

            state._fsp--;

             current =iv_ruleShortOutputPort; 
            match(input,EOF,FOLLOW_2); 

            }

        }

            catch (RecognitionException re) {
                recover(input,re);
                appendSkippedTokens();
            }
        finally {
        }
        return current;
    }
    // $ANTLR end "entryRuleShortOutputPort"


    // $ANTLR start "ruleShortOutputPort"
    // InternalFSMDSL.g:1394:1: ruleShortOutputPort returns [EObject current=null] : ( () ( (lv_name_1_0= RULE_ID ) ) (otherlv_2= '[' ( (lv_width_3_0= RULE_INT ) ) otherlv_4= ']' )? ) ;
    public final EObject ruleShortOutputPort() throws RecognitionException {
        EObject current = null;

        Token lv_name_1_0=null;
        Token otherlv_2=null;
        Token lv_width_3_0=null;
        Token otherlv_4=null;


        	enterRule();

        try {
            // InternalFSMDSL.g:1400:2: ( ( () ( (lv_name_1_0= RULE_ID ) ) (otherlv_2= '[' ( (lv_width_3_0= RULE_INT ) ) otherlv_4= ']' )? ) )
            // InternalFSMDSL.g:1401:2: ( () ( (lv_name_1_0= RULE_ID ) ) (otherlv_2= '[' ( (lv_width_3_0= RULE_INT ) ) otherlv_4= ']' )? )
            {
            // InternalFSMDSL.g:1401:2: ( () ( (lv_name_1_0= RULE_ID ) ) (otherlv_2= '[' ( (lv_width_3_0= RULE_INT ) ) otherlv_4= ']' )? )
            // InternalFSMDSL.g:1402:3: () ( (lv_name_1_0= RULE_ID ) ) (otherlv_2= '[' ( (lv_width_3_0= RULE_INT ) ) otherlv_4= ']' )?
            {
            // InternalFSMDSL.g:1402:3: ()
            // InternalFSMDSL.g:1403:4: 
            {

            				current = forceCreateModelElement(
            					grammarAccess.getShortOutputPortAccess().getOutputPortAction_0(),
            					current);
            			

            }

            // InternalFSMDSL.g:1409:3: ( (lv_name_1_0= RULE_ID ) )
            // InternalFSMDSL.g:1410:4: (lv_name_1_0= RULE_ID )
            {
            // InternalFSMDSL.g:1410:4: (lv_name_1_0= RULE_ID )
            // InternalFSMDSL.g:1411:5: lv_name_1_0= RULE_ID
            {
            lv_name_1_0=(Token)match(input,RULE_ID,FOLLOW_28); 

            					newLeafNode(lv_name_1_0, grammarAccess.getShortOutputPortAccess().getNameIDTerminalRuleCall_1_0());
            				

            					if (current==null) {
            						current = createModelElement(grammarAccess.getShortOutputPortRule());
            					}
            					setWithLastConsumed(
            						current,
            						"name",
            						lv_name_1_0,
            						"org.eclipse.xtext.common.Terminals.ID");
            				

            }


            }

            // InternalFSMDSL.g:1427:3: (otherlv_2= '[' ( (lv_width_3_0= RULE_INT ) ) otherlv_4= ']' )?
            int alt33=2;
            int LA33_0 = input.LA(1);

            if ( (LA33_0==13) ) {
                alt33=1;
            }
            switch (alt33) {
                case 1 :
                    // InternalFSMDSL.g:1428:4: otherlv_2= '[' ( (lv_width_3_0= RULE_INT ) ) otherlv_4= ']'
                    {
                    otherlv_2=(Token)match(input,13,FOLLOW_25); 

                    				newLeafNode(otherlv_2, grammarAccess.getShortOutputPortAccess().getLeftSquareBracketKeyword_2_0());
                    			
                    // InternalFSMDSL.g:1432:4: ( (lv_width_3_0= RULE_INT ) )
                    // InternalFSMDSL.g:1433:5: (lv_width_3_0= RULE_INT )
                    {
                    // InternalFSMDSL.g:1433:5: (lv_width_3_0= RULE_INT )
                    // InternalFSMDSL.g:1434:6: lv_width_3_0= RULE_INT
                    {
                    lv_width_3_0=(Token)match(input,RULE_INT,FOLLOW_29); 

                    						newLeafNode(lv_width_3_0, grammarAccess.getShortOutputPortAccess().getWidthINTTerminalRuleCall_2_1_0());
                    					

                    						if (current==null) {
                    							current = createModelElement(grammarAccess.getShortOutputPortRule());
                    						}
                    						setWithLastConsumed(
                    							current,
                    							"width",
                    							lv_width_3_0,
                    							"org.eclipse.xtext.common.Terminals.INT");
                    					

                    }


                    }

                    otherlv_4=(Token)match(input,15,FOLLOW_2); 

                    				newLeafNode(otherlv_4, grammarAccess.getShortOutputPortAccess().getRightSquareBracketKeyword_2_2());
                    			

                    }
                    break;

            }


            }


            }


            	leaveRule();

        }

            catch (RecognitionException re) {
                recover(input,re);
                appendSkippedTokens();
            }
        finally {
        }
        return current;
    }
    // $ANTLR end "ruleShortOutputPort"


    // $ANTLR start "entryRuleInputPort"
    // InternalFSMDSL.g:1459:1: entryRuleInputPort returns [EObject current=null] : iv_ruleInputPort= ruleInputPort EOF ;
    public final EObject entryRuleInputPort() throws RecognitionException {
        EObject current = null;

        EObject iv_ruleInputPort = null;


        try {
            // InternalFSMDSL.g:1459:50: (iv_ruleInputPort= ruleInputPort EOF )
            // InternalFSMDSL.g:1460:2: iv_ruleInputPort= ruleInputPort EOF
            {
             newCompositeNode(grammarAccess.getInputPortRule()); 
            pushFollow(FOLLOW_1);
            iv_ruleInputPort=ruleInputPort();

            state._fsp--;

             current =iv_ruleInputPort; 
            match(input,EOF,FOLLOW_2); 

            }

        }

            catch (RecognitionException re) {
                recover(input,re);
                appendSkippedTokens();
            }
        finally {
        }
        return current;
    }
    // $ANTLR end "entryRuleInputPort"


    // $ANTLR start "ruleInputPort"
    // InternalFSMDSL.g:1466:1: ruleInputPort returns [EObject current=null] : ( () ( (lv_name_1_0= RULE_ID ) ) (otherlv_2= '[' ( (lv_width_3_0= RULE_INT ) ) otherlv_4= ']' )? ( (lv_layout_5_0= ruleLayoutInfo ) ) otherlv_6= ';' ) ;
    public final EObject ruleInputPort() throws RecognitionException {
        EObject current = null;

        Token lv_name_1_0=null;
        Token otherlv_2=null;
        Token lv_width_3_0=null;
        Token otherlv_4=null;
        Token otherlv_6=null;
        EObject lv_layout_5_0 = null;



        	enterRule();

        try {
            // InternalFSMDSL.g:1472:2: ( ( () ( (lv_name_1_0= RULE_ID ) ) (otherlv_2= '[' ( (lv_width_3_0= RULE_INT ) ) otherlv_4= ']' )? ( (lv_layout_5_0= ruleLayoutInfo ) ) otherlv_6= ';' ) )
            // InternalFSMDSL.g:1473:2: ( () ( (lv_name_1_0= RULE_ID ) ) (otherlv_2= '[' ( (lv_width_3_0= RULE_INT ) ) otherlv_4= ']' )? ( (lv_layout_5_0= ruleLayoutInfo ) ) otherlv_6= ';' )
            {
            // InternalFSMDSL.g:1473:2: ( () ( (lv_name_1_0= RULE_ID ) ) (otherlv_2= '[' ( (lv_width_3_0= RULE_INT ) ) otherlv_4= ']' )? ( (lv_layout_5_0= ruleLayoutInfo ) ) otherlv_6= ';' )
            // InternalFSMDSL.g:1474:3: () ( (lv_name_1_0= RULE_ID ) ) (otherlv_2= '[' ( (lv_width_3_0= RULE_INT ) ) otherlv_4= ']' )? ( (lv_layout_5_0= ruleLayoutInfo ) ) otherlv_6= ';'
            {
            // InternalFSMDSL.g:1474:3: ()
            // InternalFSMDSL.g:1475:4: 
            {

            				current = forceCreateModelElement(
            					grammarAccess.getInputPortAccess().getInputPortAction_0(),
            					current);
            			

            }

            // InternalFSMDSL.g:1481:3: ( (lv_name_1_0= RULE_ID ) )
            // InternalFSMDSL.g:1482:4: (lv_name_1_0= RULE_ID )
            {
            // InternalFSMDSL.g:1482:4: (lv_name_1_0= RULE_ID )
            // InternalFSMDSL.g:1483:5: lv_name_1_0= RULE_ID
            {
            lv_name_1_0=(Token)match(input,RULE_ID,FOLLOW_30); 

            					newLeafNode(lv_name_1_0, grammarAccess.getInputPortAccess().getNameIDTerminalRuleCall_1_0());
            				

            					if (current==null) {
            						current = createModelElement(grammarAccess.getInputPortRule());
            					}
            					setWithLastConsumed(
            						current,
            						"name",
            						lv_name_1_0,
            						"org.eclipse.xtext.common.Terminals.ID");
            				

            }


            }

            // InternalFSMDSL.g:1499:3: (otherlv_2= '[' ( (lv_width_3_0= RULE_INT ) ) otherlv_4= ']' )?
            int alt34=2;
            int LA34_0 = input.LA(1);

            if ( (LA34_0==13) ) {
                alt34=1;
            }
            switch (alt34) {
                case 1 :
                    // InternalFSMDSL.g:1500:4: otherlv_2= '[' ( (lv_width_3_0= RULE_INT ) ) otherlv_4= ']'
                    {
                    otherlv_2=(Token)match(input,13,FOLLOW_25); 

                    				newLeafNode(otherlv_2, grammarAccess.getInputPortAccess().getLeftSquareBracketKeyword_2_0());
                    			
                    // InternalFSMDSL.g:1504:4: ( (lv_width_3_0= RULE_INT ) )
                    // InternalFSMDSL.g:1505:5: (lv_width_3_0= RULE_INT )
                    {
                    // InternalFSMDSL.g:1505:5: (lv_width_3_0= RULE_INT )
                    // InternalFSMDSL.g:1506:6: lv_width_3_0= RULE_INT
                    {
                    lv_width_3_0=(Token)match(input,RULE_INT,FOLLOW_29); 

                    						newLeafNode(lv_width_3_0, grammarAccess.getInputPortAccess().getWidthINTTerminalRuleCall_2_1_0());
                    					

                    						if (current==null) {
                    							current = createModelElement(grammarAccess.getInputPortRule());
                    						}
                    						setWithLastConsumed(
                    							current,
                    							"width",
                    							lv_width_3_0,
                    							"org.eclipse.xtext.common.Terminals.INT");
                    					

                    }


                    }

                    otherlv_4=(Token)match(input,15,FOLLOW_31); 

                    				newLeafNode(otherlv_4, grammarAccess.getInputPortAccess().getRightSquareBracketKeyword_2_2());
                    			

                    }
                    break;

            }

            // InternalFSMDSL.g:1527:3: ( (lv_layout_5_0= ruleLayoutInfo ) )
            // InternalFSMDSL.g:1528:4: (lv_layout_5_0= ruleLayoutInfo )
            {
            // InternalFSMDSL.g:1528:4: (lv_layout_5_0= ruleLayoutInfo )
            // InternalFSMDSL.g:1529:5: lv_layout_5_0= ruleLayoutInfo
            {

            					newCompositeNode(grammarAccess.getInputPortAccess().getLayoutLayoutInfoParserRuleCall_3_0());
            				
            pushFollow(FOLLOW_11);
            lv_layout_5_0=ruleLayoutInfo();

            state._fsp--;


            					if (current==null) {
            						current = createModelElementForParent(grammarAccess.getInputPortRule());
            					}
            					set(
            						current,
            						"layout",
            						lv_layout_5_0,
            						"com.cburch.logisim.statemachine.FSMDSL.LayoutInfo");
            					afterParserOrEnumRuleCall();
            				

            }


            }

            otherlv_6=(Token)match(input,16,FOLLOW_2); 

            			newLeafNode(otherlv_6, grammarAccess.getInputPortAccess().getSemicolonKeyword_4());
            		

            }


            }


            	leaveRule();

        }

            catch (RecognitionException re) {
                recover(input,re);
                appendSkippedTokens();
            }
        finally {
        }
        return current;
    }
    // $ANTLR end "ruleInputPort"


    // $ANTLR start "entryRuleOutputPort"
    // InternalFSMDSL.g:1554:1: entryRuleOutputPort returns [EObject current=null] : iv_ruleOutputPort= ruleOutputPort EOF ;
    public final EObject entryRuleOutputPort() throws RecognitionException {
        EObject current = null;

        EObject iv_ruleOutputPort = null;


        try {
            // InternalFSMDSL.g:1554:51: (iv_ruleOutputPort= ruleOutputPort EOF )
            // InternalFSMDSL.g:1555:2: iv_ruleOutputPort= ruleOutputPort EOF
            {
             newCompositeNode(grammarAccess.getOutputPortRule()); 
            pushFollow(FOLLOW_1);
            iv_ruleOutputPort=ruleOutputPort();

            state._fsp--;

             current =iv_ruleOutputPort; 
            match(input,EOF,FOLLOW_2); 

            }

        }

            catch (RecognitionException re) {
                recover(input,re);
                appendSkippedTokens();
            }
        finally {
        }
        return current;
    }
    // $ANTLR end "entryRuleOutputPort"


    // $ANTLR start "ruleOutputPort"
    // InternalFSMDSL.g:1561:1: ruleOutputPort returns [EObject current=null] : ( () ( (lv_name_1_0= RULE_ID ) ) (otherlv_2= '[' ( (lv_width_3_0= RULE_INT ) ) otherlv_4= ']' )? ( (lv_layout_5_0= ruleLayoutInfo ) ) otherlv_6= ';' ) ;
    public final EObject ruleOutputPort() throws RecognitionException {
        EObject current = null;

        Token lv_name_1_0=null;
        Token otherlv_2=null;
        Token lv_width_3_0=null;
        Token otherlv_4=null;
        Token otherlv_6=null;
        EObject lv_layout_5_0 = null;



        	enterRule();

        try {
            // InternalFSMDSL.g:1567:2: ( ( () ( (lv_name_1_0= RULE_ID ) ) (otherlv_2= '[' ( (lv_width_3_0= RULE_INT ) ) otherlv_4= ']' )? ( (lv_layout_5_0= ruleLayoutInfo ) ) otherlv_6= ';' ) )
            // InternalFSMDSL.g:1568:2: ( () ( (lv_name_1_0= RULE_ID ) ) (otherlv_2= '[' ( (lv_width_3_0= RULE_INT ) ) otherlv_4= ']' )? ( (lv_layout_5_0= ruleLayoutInfo ) ) otherlv_6= ';' )
            {
            // InternalFSMDSL.g:1568:2: ( () ( (lv_name_1_0= RULE_ID ) ) (otherlv_2= '[' ( (lv_width_3_0= RULE_INT ) ) otherlv_4= ']' )? ( (lv_layout_5_0= ruleLayoutInfo ) ) otherlv_6= ';' )
            // InternalFSMDSL.g:1569:3: () ( (lv_name_1_0= RULE_ID ) ) (otherlv_2= '[' ( (lv_width_3_0= RULE_INT ) ) otherlv_4= ']' )? ( (lv_layout_5_0= ruleLayoutInfo ) ) otherlv_6= ';'
            {
            // InternalFSMDSL.g:1569:3: ()
            // InternalFSMDSL.g:1570:4: 
            {

            				current = forceCreateModelElement(
            					grammarAccess.getOutputPortAccess().getOutputPortAction_0(),
            					current);
            			

            }

            // InternalFSMDSL.g:1576:3: ( (lv_name_1_0= RULE_ID ) )
            // InternalFSMDSL.g:1577:4: (lv_name_1_0= RULE_ID )
            {
            // InternalFSMDSL.g:1577:4: (lv_name_1_0= RULE_ID )
            // InternalFSMDSL.g:1578:5: lv_name_1_0= RULE_ID
            {
            lv_name_1_0=(Token)match(input,RULE_ID,FOLLOW_30); 

            					newLeafNode(lv_name_1_0, grammarAccess.getOutputPortAccess().getNameIDTerminalRuleCall_1_0());
            				

            					if (current==null) {
            						current = createModelElement(grammarAccess.getOutputPortRule());
            					}
            					setWithLastConsumed(
            						current,
            						"name",
            						lv_name_1_0,
            						"org.eclipse.xtext.common.Terminals.ID");
            				

            }


            }

            // InternalFSMDSL.g:1594:3: (otherlv_2= '[' ( (lv_width_3_0= RULE_INT ) ) otherlv_4= ']' )?
            int alt35=2;
            int LA35_0 = input.LA(1);

            if ( (LA35_0==13) ) {
                alt35=1;
            }
            switch (alt35) {
                case 1 :
                    // InternalFSMDSL.g:1595:4: otherlv_2= '[' ( (lv_width_3_0= RULE_INT ) ) otherlv_4= ']'
                    {
                    otherlv_2=(Token)match(input,13,FOLLOW_25); 

                    				newLeafNode(otherlv_2, grammarAccess.getOutputPortAccess().getLeftSquareBracketKeyword_2_0());
                    			
                    // InternalFSMDSL.g:1599:4: ( (lv_width_3_0= RULE_INT ) )
                    // InternalFSMDSL.g:1600:5: (lv_width_3_0= RULE_INT )
                    {
                    // InternalFSMDSL.g:1600:5: (lv_width_3_0= RULE_INT )
                    // InternalFSMDSL.g:1601:6: lv_width_3_0= RULE_INT
                    {
                    lv_width_3_0=(Token)match(input,RULE_INT,FOLLOW_29); 

                    						newLeafNode(lv_width_3_0, grammarAccess.getOutputPortAccess().getWidthINTTerminalRuleCall_2_1_0());
                    					

                    						if (current==null) {
                    							current = createModelElement(grammarAccess.getOutputPortRule());
                    						}
                    						setWithLastConsumed(
                    							current,
                    							"width",
                    							lv_width_3_0,
                    							"org.eclipse.xtext.common.Terminals.INT");
                    					

                    }


                    }

                    otherlv_4=(Token)match(input,15,FOLLOW_31); 

                    				newLeafNode(otherlv_4, grammarAccess.getOutputPortAccess().getRightSquareBracketKeyword_2_2());
                    			

                    }
                    break;

            }

            // InternalFSMDSL.g:1622:3: ( (lv_layout_5_0= ruleLayoutInfo ) )
            // InternalFSMDSL.g:1623:4: (lv_layout_5_0= ruleLayoutInfo )
            {
            // InternalFSMDSL.g:1623:4: (lv_layout_5_0= ruleLayoutInfo )
            // InternalFSMDSL.g:1624:5: lv_layout_5_0= ruleLayoutInfo
            {

            					newCompositeNode(grammarAccess.getOutputPortAccess().getLayoutLayoutInfoParserRuleCall_3_0());
            				
            pushFollow(FOLLOW_11);
            lv_layout_5_0=ruleLayoutInfo();

            state._fsp--;


            					if (current==null) {
            						current = createModelElementForParent(grammarAccess.getOutputPortRule());
            					}
            					set(
            						current,
            						"layout",
            						lv_layout_5_0,
            						"com.cburch.logisim.statemachine.FSMDSL.LayoutInfo");
            					afterParserOrEnumRuleCall();
            				

            }


            }

            otherlv_6=(Token)match(input,16,FOLLOW_2); 

            			newLeafNode(otherlv_6, grammarAccess.getOutputPortAccess().getSemicolonKeyword_4());
            		

            }


            }


            	leaveRule();

        }

            catch (RecognitionException re) {
                recover(input,re);
                appendSkippedTokens();
            }
        finally {
        }
        return current;
    }
    // $ANTLR end "ruleOutputPort"


    // $ANTLR start "entryRuleCommandList"
    // InternalFSMDSL.g:1649:1: entryRuleCommandList returns [EObject current=null] : iv_ruleCommandList= ruleCommandList EOF ;
    public final EObject entryRuleCommandList() throws RecognitionException {
        EObject current = null;

        EObject iv_ruleCommandList = null;


        try {
            // InternalFSMDSL.g:1649:52: (iv_ruleCommandList= ruleCommandList EOF )
            // InternalFSMDSL.g:1650:2: iv_ruleCommandList= ruleCommandList EOF
            {
             newCompositeNode(grammarAccess.getCommandListRule()); 
            pushFollow(FOLLOW_1);
            iv_ruleCommandList=ruleCommandList();

            state._fsp--;

             current =iv_ruleCommandList; 
            match(input,EOF,FOLLOW_2); 

            }

        }

            catch (RecognitionException re) {
                recover(input,re);
                appendSkippedTokens();
            }
        finally {
        }
        return current;
    }
    // $ANTLR end "entryRuleCommandList"


    // $ANTLR start "ruleCommandList"
    // InternalFSMDSL.g:1656:1: ruleCommandList returns [EObject current=null] : ( () (otherlv_1= 'commands' ( (lv_layout_2_0= ruleLayoutInfo ) ) otherlv_3= '{' ( (lv_commands_4_0= ruleCommand ) )* otherlv_5= '}' ) ) ;
    public final EObject ruleCommandList() throws RecognitionException {
        EObject current = null;

        Token otherlv_1=null;
        Token otherlv_3=null;
        Token otherlv_5=null;
        EObject lv_layout_2_0 = null;

        EObject lv_commands_4_0 = null;



        	enterRule();

        try {
            // InternalFSMDSL.g:1662:2: ( ( () (otherlv_1= 'commands' ( (lv_layout_2_0= ruleLayoutInfo ) ) otherlv_3= '{' ( (lv_commands_4_0= ruleCommand ) )* otherlv_5= '}' ) ) )
            // InternalFSMDSL.g:1663:2: ( () (otherlv_1= 'commands' ( (lv_layout_2_0= ruleLayoutInfo ) ) otherlv_3= '{' ( (lv_commands_4_0= ruleCommand ) )* otherlv_5= '}' ) )
            {
            // InternalFSMDSL.g:1663:2: ( () (otherlv_1= 'commands' ( (lv_layout_2_0= ruleLayoutInfo ) ) otherlv_3= '{' ( (lv_commands_4_0= ruleCommand ) )* otherlv_5= '}' ) )
            // InternalFSMDSL.g:1664:3: () (otherlv_1= 'commands' ( (lv_layout_2_0= ruleLayoutInfo ) ) otherlv_3= '{' ( (lv_commands_4_0= ruleCommand ) )* otherlv_5= '}' )
            {
            // InternalFSMDSL.g:1664:3: ()
            // InternalFSMDSL.g:1665:4: 
            {

            				current = forceCreateModelElement(
            					grammarAccess.getCommandListAccess().getCommandListAction_0(),
            					current);
            			

            }

            // InternalFSMDSL.g:1671:3: (otherlv_1= 'commands' ( (lv_layout_2_0= ruleLayoutInfo ) ) otherlv_3= '{' ( (lv_commands_4_0= ruleCommand ) )* otherlv_5= '}' )
            // InternalFSMDSL.g:1672:4: otherlv_1= 'commands' ( (lv_layout_2_0= ruleLayoutInfo ) ) otherlv_3= '{' ( (lv_commands_4_0= ruleCommand ) )* otherlv_5= '}'
            {
            otherlv_1=(Token)match(input,33,FOLLOW_21); 

            				newLeafNode(otherlv_1, grammarAccess.getCommandListAccess().getCommandsKeyword_1_0());
            			
            // InternalFSMDSL.g:1676:4: ( (lv_layout_2_0= ruleLayoutInfo ) )
            // InternalFSMDSL.g:1677:5: (lv_layout_2_0= ruleLayoutInfo )
            {
            // InternalFSMDSL.g:1677:5: (lv_layout_2_0= ruleLayoutInfo )
            // InternalFSMDSL.g:1678:6: lv_layout_2_0= ruleLayoutInfo
            {

            						newCompositeNode(grammarAccess.getCommandListAccess().getLayoutLayoutInfoParserRuleCall_1_1_0());
            					
            pushFollow(FOLLOW_22);
            lv_layout_2_0=ruleLayoutInfo();

            state._fsp--;


            						if (current==null) {
            							current = createModelElementForParent(grammarAccess.getCommandListRule());
            						}
            						set(
            							current,
            							"layout",
            							lv_layout_2_0,
            							"com.cburch.logisim.statemachine.FSMDSL.LayoutInfo");
            						afterParserOrEnumRuleCall();
            					

            }


            }

            otherlv_3=(Token)match(input,27,FOLLOW_32); 

            				newLeafNode(otherlv_3, grammarAccess.getCommandListAccess().getLeftCurlyBracketKeyword_1_2());
            			
            // InternalFSMDSL.g:1699:4: ( (lv_commands_4_0= ruleCommand ) )*
            loop36:
            do {
                int alt36=2;
                int LA36_0 = input.LA(1);

                if ( (LA36_0==RULE_ID) ) {
                    alt36=1;
                }


                switch (alt36) {
            	case 1 :
            	    // InternalFSMDSL.g:1700:5: (lv_commands_4_0= ruleCommand )
            	    {
            	    // InternalFSMDSL.g:1700:5: (lv_commands_4_0= ruleCommand )
            	    // InternalFSMDSL.g:1701:6: lv_commands_4_0= ruleCommand
            	    {

            	    						newCompositeNode(grammarAccess.getCommandListAccess().getCommandsCommandParserRuleCall_1_3_0());
            	    					
            	    pushFollow(FOLLOW_32);
            	    lv_commands_4_0=ruleCommand();

            	    state._fsp--;


            	    						if (current==null) {
            	    							current = createModelElementForParent(grammarAccess.getCommandListRule());
            	    						}
            	    						add(
            	    							current,
            	    							"commands",
            	    							lv_commands_4_0,
            	    							"com.cburch.logisim.statemachine.FSMDSL.Command");
            	    						afterParserOrEnumRuleCall();
            	    					

            	    }


            	    }
            	    break;

            	default :
            	    break loop36;
                }
            } while (true);

            otherlv_5=(Token)match(input,32,FOLLOW_2); 

            				newLeafNode(otherlv_5, grammarAccess.getCommandListAccess().getRightCurlyBracketKeyword_1_4());
            			

            }


            }


            }


            	leaveRule();

        }

            catch (RecognitionException re) {
                recover(input,re);
                appendSkippedTokens();
            }
        finally {
        }
        return current;
    }
    // $ANTLR end "ruleCommandList"


    // $ANTLR start "entryRuleLayoutInfo"
    // InternalFSMDSL.g:1727:1: entryRuleLayoutInfo returns [EObject current=null] : iv_ruleLayoutInfo= ruleLayoutInfo EOF ;
    public final EObject entryRuleLayoutInfo() throws RecognitionException {
        EObject current = null;

        EObject iv_ruleLayoutInfo = null;


        try {
            // InternalFSMDSL.g:1727:51: (iv_ruleLayoutInfo= ruleLayoutInfo EOF )
            // InternalFSMDSL.g:1728:2: iv_ruleLayoutInfo= ruleLayoutInfo EOF
            {
             newCompositeNode(grammarAccess.getLayoutInfoRule()); 
            pushFollow(FOLLOW_1);
            iv_ruleLayoutInfo=ruleLayoutInfo();

            state._fsp--;

             current =iv_ruleLayoutInfo; 
            match(input,EOF,FOLLOW_2); 

            }

        }

            catch (RecognitionException re) {
                recover(input,re);
                appendSkippedTokens();
            }
        finally {
        }
        return current;
    }
    // $ANTLR end "entryRuleLayoutInfo"


    // $ANTLR start "ruleLayoutInfo"
    // InternalFSMDSL.g:1734:1: ruleLayoutInfo returns [EObject current=null] : ( () (otherlv_1= '@' otherlv_2= '[' ( (lv_x_3_0= RULE_INT ) ) otherlv_4= ',' ( (lv_y_5_0= RULE_INT ) ) (otherlv_6= ',' ( (lv_width_7_0= RULE_INT ) ) otherlv_8= ',' ( (lv_height_9_0= RULE_INT ) ) )? otherlv_10= ']' )? ) ;
    public final EObject ruleLayoutInfo() throws RecognitionException {
        EObject current = null;

        Token otherlv_1=null;
        Token otherlv_2=null;
        Token lv_x_3_0=null;
        Token otherlv_4=null;
        Token lv_y_5_0=null;
        Token otherlv_6=null;
        Token lv_width_7_0=null;
        Token otherlv_8=null;
        Token lv_height_9_0=null;
        Token otherlv_10=null;


        	enterRule();

        try {
            // InternalFSMDSL.g:1740:2: ( ( () (otherlv_1= '@' otherlv_2= '[' ( (lv_x_3_0= RULE_INT ) ) otherlv_4= ',' ( (lv_y_5_0= RULE_INT ) ) (otherlv_6= ',' ( (lv_width_7_0= RULE_INT ) ) otherlv_8= ',' ( (lv_height_9_0= RULE_INT ) ) )? otherlv_10= ']' )? ) )
            // InternalFSMDSL.g:1741:2: ( () (otherlv_1= '@' otherlv_2= '[' ( (lv_x_3_0= RULE_INT ) ) otherlv_4= ',' ( (lv_y_5_0= RULE_INT ) ) (otherlv_6= ',' ( (lv_width_7_0= RULE_INT ) ) otherlv_8= ',' ( (lv_height_9_0= RULE_INT ) ) )? otherlv_10= ']' )? )
            {
            // InternalFSMDSL.g:1741:2: ( () (otherlv_1= '@' otherlv_2= '[' ( (lv_x_3_0= RULE_INT ) ) otherlv_4= ',' ( (lv_y_5_0= RULE_INT ) ) (otherlv_6= ',' ( (lv_width_7_0= RULE_INT ) ) otherlv_8= ',' ( (lv_height_9_0= RULE_INT ) ) )? otherlv_10= ']' )? )
            // InternalFSMDSL.g:1742:3: () (otherlv_1= '@' otherlv_2= '[' ( (lv_x_3_0= RULE_INT ) ) otherlv_4= ',' ( (lv_y_5_0= RULE_INT ) ) (otherlv_6= ',' ( (lv_width_7_0= RULE_INT ) ) otherlv_8= ',' ( (lv_height_9_0= RULE_INT ) ) )? otherlv_10= ']' )?
            {
            // InternalFSMDSL.g:1742:3: ()
            // InternalFSMDSL.g:1743:4: 
            {

            				current = forceCreateModelElement(
            					grammarAccess.getLayoutInfoAccess().getLayoutInfoAction_0(),
            					current);
            			

            }

            // InternalFSMDSL.g:1749:3: (otherlv_1= '@' otherlv_2= '[' ( (lv_x_3_0= RULE_INT ) ) otherlv_4= ',' ( (lv_y_5_0= RULE_INT ) ) (otherlv_6= ',' ( (lv_width_7_0= RULE_INT ) ) otherlv_8= ',' ( (lv_height_9_0= RULE_INT ) ) )? otherlv_10= ']' )?
            int alt38=2;
            int LA38_0 = input.LA(1);

            if ( (LA38_0==34) ) {
                alt38=1;
            }
            switch (alt38) {
                case 1 :
                    // InternalFSMDSL.g:1750:4: otherlv_1= '@' otherlv_2= '[' ( (lv_x_3_0= RULE_INT ) ) otherlv_4= ',' ( (lv_y_5_0= RULE_INT ) ) (otherlv_6= ',' ( (lv_width_7_0= RULE_INT ) ) otherlv_8= ',' ( (lv_height_9_0= RULE_INT ) ) )? otherlv_10= ']'
                    {
                    otherlv_1=(Token)match(input,34,FOLLOW_6); 

                    				newLeafNode(otherlv_1, grammarAccess.getLayoutInfoAccess().getCommercialAtKeyword_1_0());
                    			
                    otherlv_2=(Token)match(input,13,FOLLOW_25); 

                    				newLeafNode(otherlv_2, grammarAccess.getLayoutInfoAccess().getLeftSquareBracketKeyword_1_1());
                    			
                    // InternalFSMDSL.g:1758:4: ( (lv_x_3_0= RULE_INT ) )
                    // InternalFSMDSL.g:1759:5: (lv_x_3_0= RULE_INT )
                    {
                    // InternalFSMDSL.g:1759:5: (lv_x_3_0= RULE_INT )
                    // InternalFSMDSL.g:1760:6: lv_x_3_0= RULE_INT
                    {
                    lv_x_3_0=(Token)match(input,RULE_INT,FOLLOW_33); 

                    						newLeafNode(lv_x_3_0, grammarAccess.getLayoutInfoAccess().getXINTTerminalRuleCall_1_2_0());
                    					

                    						if (current==null) {
                    							current = createModelElement(grammarAccess.getLayoutInfoRule());
                    						}
                    						setWithLastConsumed(
                    							current,
                    							"x",
                    							lv_x_3_0,
                    							"org.eclipse.xtext.common.Terminals.INT");
                    					

                    }


                    }

                    otherlv_4=(Token)match(input,14,FOLLOW_25); 

                    				newLeafNode(otherlv_4, grammarAccess.getLayoutInfoAccess().getCommaKeyword_1_3());
                    			
                    // InternalFSMDSL.g:1780:4: ( (lv_y_5_0= RULE_INT ) )
                    // InternalFSMDSL.g:1781:5: (lv_y_5_0= RULE_INT )
                    {
                    // InternalFSMDSL.g:1781:5: (lv_y_5_0= RULE_INT )
                    // InternalFSMDSL.g:1782:6: lv_y_5_0= RULE_INT
                    {
                    lv_y_5_0=(Token)match(input,RULE_INT,FOLLOW_4); 

                    						newLeafNode(lv_y_5_0, grammarAccess.getLayoutInfoAccess().getYINTTerminalRuleCall_1_4_0());
                    					

                    						if (current==null) {
                    							current = createModelElement(grammarAccess.getLayoutInfoRule());
                    						}
                    						setWithLastConsumed(
                    							current,
                    							"y",
                    							lv_y_5_0,
                    							"org.eclipse.xtext.common.Terminals.INT");
                    					

                    }


                    }

                    // InternalFSMDSL.g:1798:4: (otherlv_6= ',' ( (lv_width_7_0= RULE_INT ) ) otherlv_8= ',' ( (lv_height_9_0= RULE_INT ) ) )?
                    int alt37=2;
                    int LA37_0 = input.LA(1);

                    if ( (LA37_0==14) ) {
                        alt37=1;
                    }
                    switch (alt37) {
                        case 1 :
                            // InternalFSMDSL.g:1799:5: otherlv_6= ',' ( (lv_width_7_0= RULE_INT ) ) otherlv_8= ',' ( (lv_height_9_0= RULE_INT ) )
                            {
                            otherlv_6=(Token)match(input,14,FOLLOW_25); 

                            					newLeafNode(otherlv_6, grammarAccess.getLayoutInfoAccess().getCommaKeyword_1_5_0());
                            				
                            // InternalFSMDSL.g:1803:5: ( (lv_width_7_0= RULE_INT ) )
                            // InternalFSMDSL.g:1804:6: (lv_width_7_0= RULE_INT )
                            {
                            // InternalFSMDSL.g:1804:6: (lv_width_7_0= RULE_INT )
                            // InternalFSMDSL.g:1805:7: lv_width_7_0= RULE_INT
                            {
                            lv_width_7_0=(Token)match(input,RULE_INT,FOLLOW_33); 

                            							newLeafNode(lv_width_7_0, grammarAccess.getLayoutInfoAccess().getWidthINTTerminalRuleCall_1_5_1_0());
                            						

                            							if (current==null) {
                            								current = createModelElement(grammarAccess.getLayoutInfoRule());
                            							}
                            							setWithLastConsumed(
                            								current,
                            								"width",
                            								lv_width_7_0,
                            								"org.eclipse.xtext.common.Terminals.INT");
                            						

                            }


                            }

                            otherlv_8=(Token)match(input,14,FOLLOW_25); 

                            					newLeafNode(otherlv_8, grammarAccess.getLayoutInfoAccess().getCommaKeyword_1_5_2());
                            				
                            // InternalFSMDSL.g:1825:5: ( (lv_height_9_0= RULE_INT ) )
                            // InternalFSMDSL.g:1826:6: (lv_height_9_0= RULE_INT )
                            {
                            // InternalFSMDSL.g:1826:6: (lv_height_9_0= RULE_INT )
                            // InternalFSMDSL.g:1827:7: lv_height_9_0= RULE_INT
                            {
                            lv_height_9_0=(Token)match(input,RULE_INT,FOLLOW_29); 

                            							newLeafNode(lv_height_9_0, grammarAccess.getLayoutInfoAccess().getHeightINTTerminalRuleCall_1_5_3_0());
                            						

                            							if (current==null) {
                            								current = createModelElement(grammarAccess.getLayoutInfoRule());
                            							}
                            							setWithLastConsumed(
                            								current,
                            								"height",
                            								lv_height_9_0,
                            								"org.eclipse.xtext.common.Terminals.INT");
                            						

                            }


                            }


                            }
                            break;

                    }

                    otherlv_10=(Token)match(input,15,FOLLOW_2); 

                    				newLeafNode(otherlv_10, grammarAccess.getLayoutInfoAccess().getRightSquareBracketKeyword_1_6());
                    			

                    }
                    break;

            }


            }


            }


            	leaveRule();

        }

            catch (RecognitionException re) {
                recover(input,re);
                appendSkippedTokens();
            }
        finally {
        }
        return current;
    }
    // $ANTLR end "ruleLayoutInfo"


    // $ANTLR start "entryRuleLongState"
    // InternalFSMDSL.g:1853:1: entryRuleLongState returns [EObject current=null] : iv_ruleLongState= ruleLongState EOF ;
    public final EObject entryRuleLongState() throws RecognitionException {
        EObject current = null;

        EObject iv_ruleLongState = null;


        try {
            // InternalFSMDSL.g:1853:50: (iv_ruleLongState= ruleLongState EOF )
            // InternalFSMDSL.g:1854:2: iv_ruleLongState= ruleLongState EOF
            {
             newCompositeNode(grammarAccess.getLongStateRule()); 
            pushFollow(FOLLOW_1);
            iv_ruleLongState=ruleLongState();

            state._fsp--;

             current =iv_ruleLongState; 
            match(input,EOF,FOLLOW_2); 

            }

        }

            catch (RecognitionException re) {
                recover(input,re);
                appendSkippedTokens();
            }
        finally {
        }
        return current;
    }
    // $ANTLR end "entryRuleLongState"


    // $ANTLR start "ruleLongState"
    // InternalFSMDSL.g:1860:1: ruleLongState returns [EObject current=null] : ( () otherlv_1= 'state' ( (lv_name_2_0= RULE_ID ) ) (otherlv_3= '=' ( (lv_code_4_0= RULE_BIN ) ) )? ( (lv_layout_5_0= ruleLayoutInfo ) ) otherlv_6= '{' ( (lv_commandList_7_0= ruleCommandList ) )? (otherlv_8= 'transitions' otherlv_9= '{' ( (lv_transition_10_0= ruleDotTransition ) )* otherlv_11= '}' )? otherlv_12= '}' ) ;
    public final EObject ruleLongState() throws RecognitionException {
        EObject current = null;

        Token otherlv_1=null;
        Token lv_name_2_0=null;
        Token otherlv_3=null;
        Token lv_code_4_0=null;
        Token otherlv_6=null;
        Token otherlv_8=null;
        Token otherlv_9=null;
        Token otherlv_11=null;
        Token otherlv_12=null;
        EObject lv_layout_5_0 = null;

        EObject lv_commandList_7_0 = null;

        EObject lv_transition_10_0 = null;



        	enterRule();

        try {
            // InternalFSMDSL.g:1866:2: ( ( () otherlv_1= 'state' ( (lv_name_2_0= RULE_ID ) ) (otherlv_3= '=' ( (lv_code_4_0= RULE_BIN ) ) )? ( (lv_layout_5_0= ruleLayoutInfo ) ) otherlv_6= '{' ( (lv_commandList_7_0= ruleCommandList ) )? (otherlv_8= 'transitions' otherlv_9= '{' ( (lv_transition_10_0= ruleDotTransition ) )* otherlv_11= '}' )? otherlv_12= '}' ) )
            // InternalFSMDSL.g:1867:2: ( () otherlv_1= 'state' ( (lv_name_2_0= RULE_ID ) ) (otherlv_3= '=' ( (lv_code_4_0= RULE_BIN ) ) )? ( (lv_layout_5_0= ruleLayoutInfo ) ) otherlv_6= '{' ( (lv_commandList_7_0= ruleCommandList ) )? (otherlv_8= 'transitions' otherlv_9= '{' ( (lv_transition_10_0= ruleDotTransition ) )* otherlv_11= '}' )? otherlv_12= '}' )
            {
            // InternalFSMDSL.g:1867:2: ( () otherlv_1= 'state' ( (lv_name_2_0= RULE_ID ) ) (otherlv_3= '=' ( (lv_code_4_0= RULE_BIN ) ) )? ( (lv_layout_5_0= ruleLayoutInfo ) ) otherlv_6= '{' ( (lv_commandList_7_0= ruleCommandList ) )? (otherlv_8= 'transitions' otherlv_9= '{' ( (lv_transition_10_0= ruleDotTransition ) )* otherlv_11= '}' )? otherlv_12= '}' )
            // InternalFSMDSL.g:1868:3: () otherlv_1= 'state' ( (lv_name_2_0= RULE_ID ) ) (otherlv_3= '=' ( (lv_code_4_0= RULE_BIN ) ) )? ( (lv_layout_5_0= ruleLayoutInfo ) ) otherlv_6= '{' ( (lv_commandList_7_0= ruleCommandList ) )? (otherlv_8= 'transitions' otherlv_9= '{' ( (lv_transition_10_0= ruleDotTransition ) )* otherlv_11= '}' )? otherlv_12= '}'
            {
            // InternalFSMDSL.g:1868:3: ()
            // InternalFSMDSL.g:1869:4: 
            {

            				current = forceCreateModelElement(
            					grammarAccess.getLongStateAccess().getStateAction_0(),
            					current);
            			

            }

            otherlv_1=(Token)match(input,35,FOLLOW_8); 

            			newLeafNode(otherlv_1, grammarAccess.getLongStateAccess().getStateKeyword_1());
            		
            // InternalFSMDSL.g:1879:3: ( (lv_name_2_0= RULE_ID ) )
            // InternalFSMDSL.g:1880:4: (lv_name_2_0= RULE_ID )
            {
            // InternalFSMDSL.g:1880:4: (lv_name_2_0= RULE_ID )
            // InternalFSMDSL.g:1881:5: lv_name_2_0= RULE_ID
            {
            lv_name_2_0=(Token)match(input,RULE_ID,FOLLOW_34); 

            					newLeafNode(lv_name_2_0, grammarAccess.getLongStateAccess().getNameIDTerminalRuleCall_2_0());
            				

            					if (current==null) {
            						current = createModelElement(grammarAccess.getLongStateRule());
            					}
            					setWithLastConsumed(
            						current,
            						"name",
            						lv_name_2_0,
            						"org.eclipse.xtext.common.Terminals.ID");
            				

            }


            }

            // InternalFSMDSL.g:1897:3: (otherlv_3= '=' ( (lv_code_4_0= RULE_BIN ) ) )?
            int alt39=2;
            int LA39_0 = input.LA(1);

            if ( (LA39_0==18) ) {
                alt39=1;
            }
            switch (alt39) {
                case 1 :
                    // InternalFSMDSL.g:1898:4: otherlv_3= '=' ( (lv_code_4_0= RULE_BIN ) )
                    {
                    otherlv_3=(Token)match(input,18,FOLLOW_35); 

                    				newLeafNode(otherlv_3, grammarAccess.getLongStateAccess().getEqualsSignKeyword_3_0());
                    			
                    // InternalFSMDSL.g:1902:4: ( (lv_code_4_0= RULE_BIN ) )
                    // InternalFSMDSL.g:1903:5: (lv_code_4_0= RULE_BIN )
                    {
                    // InternalFSMDSL.g:1903:5: (lv_code_4_0= RULE_BIN )
                    // InternalFSMDSL.g:1904:6: lv_code_4_0= RULE_BIN
                    {
                    lv_code_4_0=(Token)match(input,RULE_BIN,FOLLOW_21); 

                    						newLeafNode(lv_code_4_0, grammarAccess.getLongStateAccess().getCodeBINTerminalRuleCall_3_1_0());
                    					

                    						if (current==null) {
                    							current = createModelElement(grammarAccess.getLongStateRule());
                    						}
                    						setWithLastConsumed(
                    							current,
                    							"code",
                    							lv_code_4_0,
                    							"com.cburch.logisim.statemachine.FSMDSL.BIN");
                    					

                    }


                    }


                    }
                    break;

            }

            // InternalFSMDSL.g:1921:3: ( (lv_layout_5_0= ruleLayoutInfo ) )
            // InternalFSMDSL.g:1922:4: (lv_layout_5_0= ruleLayoutInfo )
            {
            // InternalFSMDSL.g:1922:4: (lv_layout_5_0= ruleLayoutInfo )
            // InternalFSMDSL.g:1923:5: lv_layout_5_0= ruleLayoutInfo
            {

            					newCompositeNode(grammarAccess.getLongStateAccess().getLayoutLayoutInfoParserRuleCall_4_0());
            				
            pushFollow(FOLLOW_22);
            lv_layout_5_0=ruleLayoutInfo();

            state._fsp--;


            					if (current==null) {
            						current = createModelElementForParent(grammarAccess.getLongStateRule());
            					}
            					set(
            						current,
            						"layout",
            						lv_layout_5_0,
            						"com.cburch.logisim.statemachine.FSMDSL.LayoutInfo");
            					afterParserOrEnumRuleCall();
            				

            }


            }

            otherlv_6=(Token)match(input,27,FOLLOW_36); 

            			newLeafNode(otherlv_6, grammarAccess.getLongStateAccess().getLeftCurlyBracketKeyword_5());
            		
            // InternalFSMDSL.g:1944:3: ( (lv_commandList_7_0= ruleCommandList ) )?
            int alt40=2;
            int LA40_0 = input.LA(1);

            if ( (LA40_0==33) ) {
                alt40=1;
            }
            switch (alt40) {
                case 1 :
                    // InternalFSMDSL.g:1945:4: (lv_commandList_7_0= ruleCommandList )
                    {
                    // InternalFSMDSL.g:1945:4: (lv_commandList_7_0= ruleCommandList )
                    // InternalFSMDSL.g:1946:5: lv_commandList_7_0= ruleCommandList
                    {

                    					newCompositeNode(grammarAccess.getLongStateAccess().getCommandListCommandListParserRuleCall_6_0());
                    				
                    pushFollow(FOLLOW_37);
                    lv_commandList_7_0=ruleCommandList();

                    state._fsp--;


                    					if (current==null) {
                    						current = createModelElementForParent(grammarAccess.getLongStateRule());
                    					}
                    					set(
                    						current,
                    						"commandList",
                    						lv_commandList_7_0,
                    						"com.cburch.logisim.statemachine.FSMDSL.CommandList");
                    					afterParserOrEnumRuleCall();
                    				

                    }


                    }
                    break;

            }

            // InternalFSMDSL.g:1963:3: (otherlv_8= 'transitions' otherlv_9= '{' ( (lv_transition_10_0= ruleDotTransition ) )* otherlv_11= '}' )?
            int alt42=2;
            int LA42_0 = input.LA(1);

            if ( (LA42_0==36) ) {
                alt42=1;
            }
            switch (alt42) {
                case 1 :
                    // InternalFSMDSL.g:1964:4: otherlv_8= 'transitions' otherlv_9= '{' ( (lv_transition_10_0= ruleDotTransition ) )* otherlv_11= '}'
                    {
                    otherlv_8=(Token)match(input,36,FOLLOW_22); 

                    				newLeafNode(otherlv_8, grammarAccess.getLongStateAccess().getTransitionsKeyword_7_0());
                    			
                    otherlv_9=(Token)match(input,27,FOLLOW_38); 

                    				newLeafNode(otherlv_9, grammarAccess.getLongStateAccess().getLeftCurlyBracketKeyword_7_1());
                    			
                    // InternalFSMDSL.g:1972:4: ( (lv_transition_10_0= ruleDotTransition ) )*
                    loop41:
                    do {
                        int alt41=2;
                        int LA41_0 = input.LA(1);

                        if ( (LA41_0==RULE_ID||LA41_0==41) ) {
                            alt41=1;
                        }


                        switch (alt41) {
                    	case 1 :
                    	    // InternalFSMDSL.g:1973:5: (lv_transition_10_0= ruleDotTransition )
                    	    {
                    	    // InternalFSMDSL.g:1973:5: (lv_transition_10_0= ruleDotTransition )
                    	    // InternalFSMDSL.g:1974:6: lv_transition_10_0= ruleDotTransition
                    	    {

                    	    						newCompositeNode(grammarAccess.getLongStateAccess().getTransitionDotTransitionParserRuleCall_7_2_0());
                    	    					
                    	    pushFollow(FOLLOW_38);
                    	    lv_transition_10_0=ruleDotTransition();

                    	    state._fsp--;


                    	    						if (current==null) {
                    	    							current = createModelElementForParent(grammarAccess.getLongStateRule());
                    	    						}
                    	    						add(
                    	    							current,
                    	    							"transition",
                    	    							lv_transition_10_0,
                    	    							"com.cburch.logisim.statemachine.FSMDSL.DotTransition");
                    	    						afterParserOrEnumRuleCall();
                    	    					

                    	    }


                    	    }
                    	    break;

                    	default :
                    	    break loop41;
                        }
                    } while (true);

                    otherlv_11=(Token)match(input,32,FOLLOW_39); 

                    				newLeafNode(otherlv_11, grammarAccess.getLongStateAccess().getRightCurlyBracketKeyword_7_3());
                    			

                    }
                    break;

            }

            otherlv_12=(Token)match(input,32,FOLLOW_2); 

            			newLeafNode(otherlv_12, grammarAccess.getLongStateAccess().getRightCurlyBracketKeyword_8());
            		

            }


            }


            	leaveRule();

        }

            catch (RecognitionException re) {
                recover(input,re);
                appendSkippedTokens();
            }
        finally {
        }
        return current;
    }
    // $ANTLR end "ruleLongState"


    // $ANTLR start "entryRuleShortState"
    // InternalFSMDSL.g:2004:1: entryRuleShortState returns [EObject current=null] : iv_ruleShortState= ruleShortState EOF ;
    public final EObject entryRuleShortState() throws RecognitionException {
        EObject current = null;

        EObject iv_ruleShortState = null;


        try {
            // InternalFSMDSL.g:2004:51: (iv_ruleShortState= ruleShortState EOF )
            // InternalFSMDSL.g:2005:2: iv_ruleShortState= ruleShortState EOF
            {
             newCompositeNode(grammarAccess.getShortStateRule()); 
            pushFollow(FOLLOW_1);
            iv_ruleShortState=ruleShortState();

            state._fsp--;

             current =iv_ruleShortState; 
            match(input,EOF,FOLLOW_2); 

            }

        }

            catch (RecognitionException re) {
                recover(input,re);
                appendSkippedTokens();
            }
        finally {
        }
        return current;
    }
    // $ANTLR end "entryRuleShortState"


    // $ANTLR start "ruleShortState"
    // InternalFSMDSL.g:2011:1: ruleShortState returns [EObject current=null] : (otherlv_0= 'state' ( (lv_name_1_0= RULE_ID ) ) (otherlv_2= '[' ( (lv_code_3_0= RULE_BIN ) ) otherlv_4= ']' )? otherlv_5= ':' ( (lv_layout_6_0= ruleLayoutInfo ) ) ( (lv_commandList_7_0= ruleShortCommandList ) ) ( (lv_transition_8_0= ruleGotoTransition ) )* ) ;
    public final EObject ruleShortState() throws RecognitionException {
        EObject current = null;

        Token otherlv_0=null;
        Token lv_name_1_0=null;
        Token otherlv_2=null;
        Token lv_code_3_0=null;
        Token otherlv_4=null;
        Token otherlv_5=null;
        EObject lv_layout_6_0 = null;

        EObject lv_commandList_7_0 = null;

        EObject lv_transition_8_0 = null;



        	enterRule();

        try {
            // InternalFSMDSL.g:2017:2: ( (otherlv_0= 'state' ( (lv_name_1_0= RULE_ID ) ) (otherlv_2= '[' ( (lv_code_3_0= RULE_BIN ) ) otherlv_4= ']' )? otherlv_5= ':' ( (lv_layout_6_0= ruleLayoutInfo ) ) ( (lv_commandList_7_0= ruleShortCommandList ) ) ( (lv_transition_8_0= ruleGotoTransition ) )* ) )
            // InternalFSMDSL.g:2018:2: (otherlv_0= 'state' ( (lv_name_1_0= RULE_ID ) ) (otherlv_2= '[' ( (lv_code_3_0= RULE_BIN ) ) otherlv_4= ']' )? otherlv_5= ':' ( (lv_layout_6_0= ruleLayoutInfo ) ) ( (lv_commandList_7_0= ruleShortCommandList ) ) ( (lv_transition_8_0= ruleGotoTransition ) )* )
            {
            // InternalFSMDSL.g:2018:2: (otherlv_0= 'state' ( (lv_name_1_0= RULE_ID ) ) (otherlv_2= '[' ( (lv_code_3_0= RULE_BIN ) ) otherlv_4= ']' )? otherlv_5= ':' ( (lv_layout_6_0= ruleLayoutInfo ) ) ( (lv_commandList_7_0= ruleShortCommandList ) ) ( (lv_transition_8_0= ruleGotoTransition ) )* )
            // InternalFSMDSL.g:2019:3: otherlv_0= 'state' ( (lv_name_1_0= RULE_ID ) ) (otherlv_2= '[' ( (lv_code_3_0= RULE_BIN ) ) otherlv_4= ']' )? otherlv_5= ':' ( (lv_layout_6_0= ruleLayoutInfo ) ) ( (lv_commandList_7_0= ruleShortCommandList ) ) ( (lv_transition_8_0= ruleGotoTransition ) )*
            {
            otherlv_0=(Token)match(input,35,FOLLOW_8); 

            			newLeafNode(otherlv_0, grammarAccess.getShortStateAccess().getStateKeyword_0());
            		
            // InternalFSMDSL.g:2023:3: ( (lv_name_1_0= RULE_ID ) )
            // InternalFSMDSL.g:2024:4: (lv_name_1_0= RULE_ID )
            {
            // InternalFSMDSL.g:2024:4: (lv_name_1_0= RULE_ID )
            // InternalFSMDSL.g:2025:5: lv_name_1_0= RULE_ID
            {
            lv_name_1_0=(Token)match(input,RULE_ID,FOLLOW_40); 

            					newLeafNode(lv_name_1_0, grammarAccess.getShortStateAccess().getNameIDTerminalRuleCall_1_0());
            				

            					if (current==null) {
            						current = createModelElement(grammarAccess.getShortStateRule());
            					}
            					setWithLastConsumed(
            						current,
            						"name",
            						lv_name_1_0,
            						"org.eclipse.xtext.common.Terminals.ID");
            				

            }


            }

            // InternalFSMDSL.g:2041:3: (otherlv_2= '[' ( (lv_code_3_0= RULE_BIN ) ) otherlv_4= ']' )?
            int alt43=2;
            int LA43_0 = input.LA(1);

            if ( (LA43_0==13) ) {
                alt43=1;
            }
            switch (alt43) {
                case 1 :
                    // InternalFSMDSL.g:2042:4: otherlv_2= '[' ( (lv_code_3_0= RULE_BIN ) ) otherlv_4= ']'
                    {
                    otherlv_2=(Token)match(input,13,FOLLOW_35); 

                    				newLeafNode(otherlv_2, grammarAccess.getShortStateAccess().getLeftSquareBracketKeyword_2_0());
                    			
                    // InternalFSMDSL.g:2046:4: ( (lv_code_3_0= RULE_BIN ) )
                    // InternalFSMDSL.g:2047:5: (lv_code_3_0= RULE_BIN )
                    {
                    // InternalFSMDSL.g:2047:5: (lv_code_3_0= RULE_BIN )
                    // InternalFSMDSL.g:2048:6: lv_code_3_0= RULE_BIN
                    {
                    lv_code_3_0=(Token)match(input,RULE_BIN,FOLLOW_29); 

                    						newLeafNode(lv_code_3_0, grammarAccess.getShortStateAccess().getCodeBINTerminalRuleCall_2_1_0());
                    					

                    						if (current==null) {
                    							current = createModelElement(grammarAccess.getShortStateRule());
                    						}
                    						setWithLastConsumed(
                    							current,
                    							"code",
                    							lv_code_3_0,
                    							"com.cburch.logisim.statemachine.FSMDSL.BIN");
                    					

                    }


                    }

                    otherlv_4=(Token)match(input,15,FOLLOW_41); 

                    				newLeafNode(otherlv_4, grammarAccess.getShortStateAccess().getRightSquareBracketKeyword_2_2());
                    			

                    }
                    break;

            }

            otherlv_5=(Token)match(input,37,FOLLOW_42); 

            			newLeafNode(otherlv_5, grammarAccess.getShortStateAccess().getColonKeyword_3());
            		
            // InternalFSMDSL.g:2073:3: ( (lv_layout_6_0= ruleLayoutInfo ) )
            // InternalFSMDSL.g:2074:4: (lv_layout_6_0= ruleLayoutInfo )
            {
            // InternalFSMDSL.g:2074:4: (lv_layout_6_0= ruleLayoutInfo )
            // InternalFSMDSL.g:2075:5: lv_layout_6_0= ruleLayoutInfo
            {

            					newCompositeNode(grammarAccess.getShortStateAccess().getLayoutLayoutInfoParserRuleCall_4_0());
            				
            pushFollow(FOLLOW_43);
            lv_layout_6_0=ruleLayoutInfo();

            state._fsp--;


            					if (current==null) {
            						current = createModelElementForParent(grammarAccess.getShortStateRule());
            					}
            					set(
            						current,
            						"layout",
            						lv_layout_6_0,
            						"com.cburch.logisim.statemachine.FSMDSL.LayoutInfo");
            					afterParserOrEnumRuleCall();
            				

            }


            }

            // InternalFSMDSL.g:2092:3: ( (lv_commandList_7_0= ruleShortCommandList ) )
            // InternalFSMDSL.g:2093:4: (lv_commandList_7_0= ruleShortCommandList )
            {
            // InternalFSMDSL.g:2093:4: (lv_commandList_7_0= ruleShortCommandList )
            // InternalFSMDSL.g:2094:5: lv_commandList_7_0= ruleShortCommandList
            {

            					newCompositeNode(grammarAccess.getShortStateAccess().getCommandListShortCommandListParserRuleCall_5_0());
            				
            pushFollow(FOLLOW_44);
            lv_commandList_7_0=ruleShortCommandList();

            state._fsp--;


            					if (current==null) {
            						current = createModelElementForParent(grammarAccess.getShortStateRule());
            					}
            					set(
            						current,
            						"commandList",
            						lv_commandList_7_0,
            						"com.cburch.logisim.statemachine.FSMDSL.ShortCommandList");
            					afterParserOrEnumRuleCall();
            				

            }


            }

            // InternalFSMDSL.g:2111:3: ( (lv_transition_8_0= ruleGotoTransition ) )*
            loop44:
            do {
                int alt44=2;
                int LA44_0 = input.LA(1);

                if ( (LA44_0==39) ) {
                    alt44=1;
                }


                switch (alt44) {
            	case 1 :
            	    // InternalFSMDSL.g:2112:4: (lv_transition_8_0= ruleGotoTransition )
            	    {
            	    // InternalFSMDSL.g:2112:4: (lv_transition_8_0= ruleGotoTransition )
            	    // InternalFSMDSL.g:2113:5: lv_transition_8_0= ruleGotoTransition
            	    {

            	    					newCompositeNode(grammarAccess.getShortStateAccess().getTransitionGotoTransitionParserRuleCall_6_0());
            	    				
            	    pushFollow(FOLLOW_44);
            	    lv_transition_8_0=ruleGotoTransition();

            	    state._fsp--;


            	    					if (current==null) {
            	    						current = createModelElementForParent(grammarAccess.getShortStateRule());
            	    					}
            	    					add(
            	    						current,
            	    						"transition",
            	    						lv_transition_8_0,
            	    						"com.cburch.logisim.statemachine.FSMDSL.GotoTransition");
            	    					afterParserOrEnumRuleCall();
            	    				

            	    }


            	    }
            	    break;

            	default :
            	    break loop44;
                }
            } while (true);


            }


            }


            	leaveRule();

        }

            catch (RecognitionException re) {
                recover(input,re);
                appendSkippedTokens();
            }
        finally {
        }
        return current;
    }
    // $ANTLR end "ruleShortState"


    // $ANTLR start "entryRuleShortCommandList"
    // InternalFSMDSL.g:2134:1: entryRuleShortCommandList returns [EObject current=null] : iv_ruleShortCommandList= ruleShortCommandList EOF ;
    public final EObject entryRuleShortCommandList() throws RecognitionException {
        EObject current = null;

        EObject iv_ruleShortCommandList = null;


        try {
            // InternalFSMDSL.g:2134:57: (iv_ruleShortCommandList= ruleShortCommandList EOF )
            // InternalFSMDSL.g:2135:2: iv_ruleShortCommandList= ruleShortCommandList EOF
            {
             newCompositeNode(grammarAccess.getShortCommandListRule()); 
            pushFollow(FOLLOW_1);
            iv_ruleShortCommandList=ruleShortCommandList();

            state._fsp--;

             current =iv_ruleShortCommandList; 
            match(input,EOF,FOLLOW_2); 

            }

        }

            catch (RecognitionException re) {
                recover(input,re);
                appendSkippedTokens();
            }
        finally {
        }
        return current;
    }
    // $ANTLR end "entryRuleShortCommandList"


    // $ANTLR start "ruleShortCommandList"
    // InternalFSMDSL.g:2141:1: ruleShortCommandList returns [EObject current=null] : ( () (otherlv_1= 'set' ( (lv_commands_2_0= ruleCommand ) )+ ( (lv_layout_3_0= ruleLayoutInfo ) ) )? ) ;
    public final EObject ruleShortCommandList() throws RecognitionException {
        EObject current = null;

        Token otherlv_1=null;
        EObject lv_commands_2_0 = null;

        EObject lv_layout_3_0 = null;



        	enterRule();

        try {
            // InternalFSMDSL.g:2147:2: ( ( () (otherlv_1= 'set' ( (lv_commands_2_0= ruleCommand ) )+ ( (lv_layout_3_0= ruleLayoutInfo ) ) )? ) )
            // InternalFSMDSL.g:2148:2: ( () (otherlv_1= 'set' ( (lv_commands_2_0= ruleCommand ) )+ ( (lv_layout_3_0= ruleLayoutInfo ) ) )? )
            {
            // InternalFSMDSL.g:2148:2: ( () (otherlv_1= 'set' ( (lv_commands_2_0= ruleCommand ) )+ ( (lv_layout_3_0= ruleLayoutInfo ) ) )? )
            // InternalFSMDSL.g:2149:3: () (otherlv_1= 'set' ( (lv_commands_2_0= ruleCommand ) )+ ( (lv_layout_3_0= ruleLayoutInfo ) ) )?
            {
            // InternalFSMDSL.g:2149:3: ()
            // InternalFSMDSL.g:2150:4: 
            {

            				current = forceCreateModelElement(
            					grammarAccess.getShortCommandListAccess().getCommandListAction_0(),
            					current);
            			

            }

            // InternalFSMDSL.g:2156:3: (otherlv_1= 'set' ( (lv_commands_2_0= ruleCommand ) )+ ( (lv_layout_3_0= ruleLayoutInfo ) ) )?
            int alt46=2;
            int LA46_0 = input.LA(1);

            if ( (LA46_0==38) ) {
                alt46=1;
            }
            switch (alt46) {
                case 1 :
                    // InternalFSMDSL.g:2157:4: otherlv_1= 'set' ( (lv_commands_2_0= ruleCommand ) )+ ( (lv_layout_3_0= ruleLayoutInfo ) )
                    {
                    otherlv_1=(Token)match(input,38,FOLLOW_8); 

                    				newLeafNode(otherlv_1, grammarAccess.getShortCommandListAccess().getSetKeyword_1_0());
                    			
                    // InternalFSMDSL.g:2161:4: ( (lv_commands_2_0= ruleCommand ) )+
                    int cnt45=0;
                    loop45:
                    do {
                        int alt45=2;
                        int LA45_0 = input.LA(1);

                        if ( (LA45_0==RULE_ID) ) {
                            alt45=1;
                        }


                        switch (alt45) {
                    	case 1 :
                    	    // InternalFSMDSL.g:2162:5: (lv_commands_2_0= ruleCommand )
                    	    {
                    	    // InternalFSMDSL.g:2162:5: (lv_commands_2_0= ruleCommand )
                    	    // InternalFSMDSL.g:2163:6: lv_commands_2_0= ruleCommand
                    	    {

                    	    						newCompositeNode(grammarAccess.getShortCommandListAccess().getCommandsCommandParserRuleCall_1_1_0());
                    	    					
                    	    pushFollow(FOLLOW_45);
                    	    lv_commands_2_0=ruleCommand();

                    	    state._fsp--;


                    	    						if (current==null) {
                    	    							current = createModelElementForParent(grammarAccess.getShortCommandListRule());
                    	    						}
                    	    						add(
                    	    							current,
                    	    							"commands",
                    	    							lv_commands_2_0,
                    	    							"com.cburch.logisim.statemachine.FSMDSL.Command");
                    	    						afterParserOrEnumRuleCall();
                    	    					

                    	    }


                    	    }
                    	    break;

                    	default :
                    	    if ( cnt45 >= 1 ) break loop45;
                                EarlyExitException eee =
                                    new EarlyExitException(45, input);
                                throw eee;
                        }
                        cnt45++;
                    } while (true);

                    // InternalFSMDSL.g:2180:4: ( (lv_layout_3_0= ruleLayoutInfo ) )
                    // InternalFSMDSL.g:2181:5: (lv_layout_3_0= ruleLayoutInfo )
                    {
                    // InternalFSMDSL.g:2181:5: (lv_layout_3_0= ruleLayoutInfo )
                    // InternalFSMDSL.g:2182:6: lv_layout_3_0= ruleLayoutInfo
                    {

                    						newCompositeNode(grammarAccess.getShortCommandListAccess().getLayoutLayoutInfoParserRuleCall_1_2_0());
                    					
                    pushFollow(FOLLOW_2);
                    lv_layout_3_0=ruleLayoutInfo();

                    state._fsp--;


                    						if (current==null) {
                    							current = createModelElementForParent(grammarAccess.getShortCommandListRule());
                    						}
                    						set(
                    							current,
                    							"layout",
                    							lv_layout_3_0,
                    							"com.cburch.logisim.statemachine.FSMDSL.LayoutInfo");
                    						afterParserOrEnumRuleCall();
                    					

                    }


                    }


                    }
                    break;

            }


            }


            }


            	leaveRule();

        }

            catch (RecognitionException re) {
                recover(input,re);
                appendSkippedTokens();
            }
        finally {
        }
        return current;
    }
    // $ANTLR end "ruleShortCommandList"


    // $ANTLR start "entryRuleGotoTransition"
    // InternalFSMDSL.g:2204:1: entryRuleGotoTransition returns [EObject current=null] : iv_ruleGotoTransition= ruleGotoTransition EOF ;
    public final EObject entryRuleGotoTransition() throws RecognitionException {
        EObject current = null;

        EObject iv_ruleGotoTransition = null;


        try {
            // InternalFSMDSL.g:2204:55: (iv_ruleGotoTransition= ruleGotoTransition EOF )
            // InternalFSMDSL.g:2205:2: iv_ruleGotoTransition= ruleGotoTransition EOF
            {
             newCompositeNode(grammarAccess.getGotoTransitionRule()); 
            pushFollow(FOLLOW_1);
            iv_ruleGotoTransition=ruleGotoTransition();

            state._fsp--;

             current =iv_ruleGotoTransition; 
            match(input,EOF,FOLLOW_2); 

            }

        }

            catch (RecognitionException re) {
                recover(input,re);
                appendSkippedTokens();
            }
        finally {
        }
        return current;
    }
    // $ANTLR end "entryRuleGotoTransition"


    // $ANTLR start "ruleGotoTransition"
    // InternalFSMDSL.g:2211:1: ruleGotoTransition returns [EObject current=null] : (otherlv_0= 'goto' ( (otherlv_1= RULE_ID ) ) (otherlv_2= 'when' ( (lv_predicate_3_0= rulePredicate ) ) )? ( (lv_layout_4_0= ruleLayoutInfo ) ) otherlv_5= ';' ) ;
    public final EObject ruleGotoTransition() throws RecognitionException {
        EObject current = null;

        Token otherlv_0=null;
        Token otherlv_1=null;
        Token otherlv_2=null;
        Token otherlv_5=null;
        EObject lv_predicate_3_0 = null;

        EObject lv_layout_4_0 = null;



        	enterRule();

        try {
            // InternalFSMDSL.g:2217:2: ( (otherlv_0= 'goto' ( (otherlv_1= RULE_ID ) ) (otherlv_2= 'when' ( (lv_predicate_3_0= rulePredicate ) ) )? ( (lv_layout_4_0= ruleLayoutInfo ) ) otherlv_5= ';' ) )
            // InternalFSMDSL.g:2218:2: (otherlv_0= 'goto' ( (otherlv_1= RULE_ID ) ) (otherlv_2= 'when' ( (lv_predicate_3_0= rulePredicate ) ) )? ( (lv_layout_4_0= ruleLayoutInfo ) ) otherlv_5= ';' )
            {
            // InternalFSMDSL.g:2218:2: (otherlv_0= 'goto' ( (otherlv_1= RULE_ID ) ) (otherlv_2= 'when' ( (lv_predicate_3_0= rulePredicate ) ) )? ( (lv_layout_4_0= ruleLayoutInfo ) ) otherlv_5= ';' )
            // InternalFSMDSL.g:2219:3: otherlv_0= 'goto' ( (otherlv_1= RULE_ID ) ) (otherlv_2= 'when' ( (lv_predicate_3_0= rulePredicate ) ) )? ( (lv_layout_4_0= ruleLayoutInfo ) ) otherlv_5= ';'
            {
            otherlv_0=(Token)match(input,39,FOLLOW_8); 

            			newLeafNode(otherlv_0, grammarAccess.getGotoTransitionAccess().getGotoKeyword_0());
            		
            // InternalFSMDSL.g:2223:3: ( (otherlv_1= RULE_ID ) )
            // InternalFSMDSL.g:2224:4: (otherlv_1= RULE_ID )
            {
            // InternalFSMDSL.g:2224:4: (otherlv_1= RULE_ID )
            // InternalFSMDSL.g:2225:5: otherlv_1= RULE_ID
            {

            					if (current==null) {
            						current = createModelElement(grammarAccess.getGotoTransitionRule());
            					}
            				
            otherlv_1=(Token)match(input,RULE_ID,FOLLOW_46); 

            					newLeafNode(otherlv_1, grammarAccess.getGotoTransitionAccess().getDstStateCrossReference_1_0());
            				

            }


            }

            // InternalFSMDSL.g:2236:3: (otherlv_2= 'when' ( (lv_predicate_3_0= rulePredicate ) ) )?
            int alt47=2;
            int LA47_0 = input.LA(1);

            if ( (LA47_0==40) ) {
                alt47=1;
            }
            switch (alt47) {
                case 1 :
                    // InternalFSMDSL.g:2237:4: otherlv_2= 'when' ( (lv_predicate_3_0= rulePredicate ) )
                    {
                    otherlv_2=(Token)match(input,40,FOLLOW_12); 

                    				newLeafNode(otherlv_2, grammarAccess.getGotoTransitionAccess().getWhenKeyword_2_0());
                    			
                    // InternalFSMDSL.g:2241:4: ( (lv_predicate_3_0= rulePredicate ) )
                    // InternalFSMDSL.g:2242:5: (lv_predicate_3_0= rulePredicate )
                    {
                    // InternalFSMDSL.g:2242:5: (lv_predicate_3_0= rulePredicate )
                    // InternalFSMDSL.g:2243:6: lv_predicate_3_0= rulePredicate
                    {

                    						newCompositeNode(grammarAccess.getGotoTransitionAccess().getPredicatePredicateParserRuleCall_2_1_0());
                    					
                    pushFollow(FOLLOW_31);
                    lv_predicate_3_0=rulePredicate();

                    state._fsp--;


                    						if (current==null) {
                    							current = createModelElementForParent(grammarAccess.getGotoTransitionRule());
                    						}
                    						set(
                    							current,
                    							"predicate",
                    							lv_predicate_3_0,
                    							"com.cburch.logisim.statemachine.FSMDSL.Predicate");
                    						afterParserOrEnumRuleCall();
                    					

                    }


                    }


                    }
                    break;

            }

            // InternalFSMDSL.g:2261:3: ( (lv_layout_4_0= ruleLayoutInfo ) )
            // InternalFSMDSL.g:2262:4: (lv_layout_4_0= ruleLayoutInfo )
            {
            // InternalFSMDSL.g:2262:4: (lv_layout_4_0= ruleLayoutInfo )
            // InternalFSMDSL.g:2263:5: lv_layout_4_0= ruleLayoutInfo
            {

            					newCompositeNode(grammarAccess.getGotoTransitionAccess().getLayoutLayoutInfoParserRuleCall_3_0());
            				
            pushFollow(FOLLOW_11);
            lv_layout_4_0=ruleLayoutInfo();

            state._fsp--;


            					if (current==null) {
            						current = createModelElementForParent(grammarAccess.getGotoTransitionRule());
            					}
            					set(
            						current,
            						"layout",
            						lv_layout_4_0,
            						"com.cburch.logisim.statemachine.FSMDSL.LayoutInfo");
            					afterParserOrEnumRuleCall();
            				

            }


            }

            otherlv_5=(Token)match(input,16,FOLLOW_2); 

            			newLeafNode(otherlv_5, grammarAccess.getGotoTransitionAccess().getSemicolonKeyword_4());
            		

            }


            }


            	leaveRule();

        }

            catch (RecognitionException re) {
                recover(input,re);
                appendSkippedTokens();
            }
        finally {
        }
        return current;
    }
    // $ANTLR end "ruleGotoTransition"


    // $ANTLR start "entryRuleDotTransition"
    // InternalFSMDSL.g:2288:1: entryRuleDotTransition returns [EObject current=null] : iv_ruleDotTransition= ruleDotTransition EOF ;
    public final EObject entryRuleDotTransition() throws RecognitionException {
        EObject current = null;

        EObject iv_ruleDotTransition = null;


        try {
            // InternalFSMDSL.g:2288:54: (iv_ruleDotTransition= ruleDotTransition EOF )
            // InternalFSMDSL.g:2289:2: iv_ruleDotTransition= ruleDotTransition EOF
            {
             newCompositeNode(grammarAccess.getDotTransitionRule()); 
            pushFollow(FOLLOW_1);
            iv_ruleDotTransition=ruleDotTransition();

            state._fsp--;

             current =iv_ruleDotTransition; 
            match(input,EOF,FOLLOW_2); 

            }

        }

            catch (RecognitionException re) {
                recover(input,re);
                appendSkippedTokens();
            }
        finally {
        }
        return current;
    }
    // $ANTLR end "entryRuleDotTransition"


    // $ANTLR start "ruleDotTransition"
    // InternalFSMDSL.g:2295:1: ruleDotTransition returns [EObject current=null] : ( ( ( (otherlv_0= RULE_ID ) )? otherlv_1= '->' ) ( (otherlv_2= RULE_ID ) ) (otherlv_3= 'when' ( (lv_predicate_4_0= rulePredicate ) ) )? ( (lv_layout_5_0= ruleLayoutInfo ) ) otherlv_6= ';' ) ;
    public final EObject ruleDotTransition() throws RecognitionException {
        EObject current = null;

        Token otherlv_0=null;
        Token otherlv_1=null;
        Token otherlv_2=null;
        Token otherlv_3=null;
        Token otherlv_6=null;
        EObject lv_predicate_4_0 = null;

        EObject lv_layout_5_0 = null;



        	enterRule();

        try {
            // InternalFSMDSL.g:2301:2: ( ( ( ( (otherlv_0= RULE_ID ) )? otherlv_1= '->' ) ( (otherlv_2= RULE_ID ) ) (otherlv_3= 'when' ( (lv_predicate_4_0= rulePredicate ) ) )? ( (lv_layout_5_0= ruleLayoutInfo ) ) otherlv_6= ';' ) )
            // InternalFSMDSL.g:2302:2: ( ( ( (otherlv_0= RULE_ID ) )? otherlv_1= '->' ) ( (otherlv_2= RULE_ID ) ) (otherlv_3= 'when' ( (lv_predicate_4_0= rulePredicate ) ) )? ( (lv_layout_5_0= ruleLayoutInfo ) ) otherlv_6= ';' )
            {
            // InternalFSMDSL.g:2302:2: ( ( ( (otherlv_0= RULE_ID ) )? otherlv_1= '->' ) ( (otherlv_2= RULE_ID ) ) (otherlv_3= 'when' ( (lv_predicate_4_0= rulePredicate ) ) )? ( (lv_layout_5_0= ruleLayoutInfo ) ) otherlv_6= ';' )
            // InternalFSMDSL.g:2303:3: ( ( (otherlv_0= RULE_ID ) )? otherlv_1= '->' ) ( (otherlv_2= RULE_ID ) ) (otherlv_3= 'when' ( (lv_predicate_4_0= rulePredicate ) ) )? ( (lv_layout_5_0= ruleLayoutInfo ) ) otherlv_6= ';'
            {
            // InternalFSMDSL.g:2303:3: ( ( (otherlv_0= RULE_ID ) )? otherlv_1= '->' )
            // InternalFSMDSL.g:2304:4: ( (otherlv_0= RULE_ID ) )? otherlv_1= '->'
            {
            // InternalFSMDSL.g:2304:4: ( (otherlv_0= RULE_ID ) )?
            int alt48=2;
            int LA48_0 = input.LA(1);

            if ( (LA48_0==RULE_ID) ) {
                alt48=1;
            }
            switch (alt48) {
                case 1 :
                    // InternalFSMDSL.g:2305:5: (otherlv_0= RULE_ID )
                    {
                    // InternalFSMDSL.g:2305:5: (otherlv_0= RULE_ID )
                    // InternalFSMDSL.g:2306:6: otherlv_0= RULE_ID
                    {

                    						if (current==null) {
                    							current = createModelElement(grammarAccess.getDotTransitionRule());
                    						}
                    					
                    otherlv_0=(Token)match(input,RULE_ID,FOLLOW_47); 

                    						newLeafNode(otherlv_0, grammarAccess.getDotTransitionAccess().getSrcStateCrossReference_0_0_0());
                    					

                    }


                    }
                    break;

            }

            otherlv_1=(Token)match(input,41,FOLLOW_8); 

            				newLeafNode(otherlv_1, grammarAccess.getDotTransitionAccess().getHyphenMinusGreaterThanSignKeyword_0_1());
            			

            }

            // InternalFSMDSL.g:2322:3: ( (otherlv_2= RULE_ID ) )
            // InternalFSMDSL.g:2323:4: (otherlv_2= RULE_ID )
            {
            // InternalFSMDSL.g:2323:4: (otherlv_2= RULE_ID )
            // InternalFSMDSL.g:2324:5: otherlv_2= RULE_ID
            {

            					if (current==null) {
            						current = createModelElement(grammarAccess.getDotTransitionRule());
            					}
            				
            otherlv_2=(Token)match(input,RULE_ID,FOLLOW_46); 

            					newLeafNode(otherlv_2, grammarAccess.getDotTransitionAccess().getDstStateCrossReference_1_0());
            				

            }


            }

            // InternalFSMDSL.g:2335:3: (otherlv_3= 'when' ( (lv_predicate_4_0= rulePredicate ) ) )?
            int alt49=2;
            int LA49_0 = input.LA(1);

            if ( (LA49_0==40) ) {
                alt49=1;
            }
            switch (alt49) {
                case 1 :
                    // InternalFSMDSL.g:2336:4: otherlv_3= 'when' ( (lv_predicate_4_0= rulePredicate ) )
                    {
                    otherlv_3=(Token)match(input,40,FOLLOW_12); 

                    				newLeafNode(otherlv_3, grammarAccess.getDotTransitionAccess().getWhenKeyword_2_0());
                    			
                    // InternalFSMDSL.g:2340:4: ( (lv_predicate_4_0= rulePredicate ) )
                    // InternalFSMDSL.g:2341:5: (lv_predicate_4_0= rulePredicate )
                    {
                    // InternalFSMDSL.g:2341:5: (lv_predicate_4_0= rulePredicate )
                    // InternalFSMDSL.g:2342:6: lv_predicate_4_0= rulePredicate
                    {

                    						newCompositeNode(grammarAccess.getDotTransitionAccess().getPredicatePredicateParserRuleCall_2_1_0());
                    					
                    pushFollow(FOLLOW_31);
                    lv_predicate_4_0=rulePredicate();

                    state._fsp--;


                    						if (current==null) {
                    							current = createModelElementForParent(grammarAccess.getDotTransitionRule());
                    						}
                    						set(
                    							current,
                    							"predicate",
                    							lv_predicate_4_0,
                    							"com.cburch.logisim.statemachine.FSMDSL.Predicate");
                    						afterParserOrEnumRuleCall();
                    					

                    }


                    }


                    }
                    break;

            }

            // InternalFSMDSL.g:2360:3: ( (lv_layout_5_0= ruleLayoutInfo ) )
            // InternalFSMDSL.g:2361:4: (lv_layout_5_0= ruleLayoutInfo )
            {
            // InternalFSMDSL.g:2361:4: (lv_layout_5_0= ruleLayoutInfo )
            // InternalFSMDSL.g:2362:5: lv_layout_5_0= ruleLayoutInfo
            {

            					newCompositeNode(grammarAccess.getDotTransitionAccess().getLayoutLayoutInfoParserRuleCall_3_0());
            				
            pushFollow(FOLLOW_11);
            lv_layout_5_0=ruleLayoutInfo();

            state._fsp--;


            					if (current==null) {
            						current = createModelElementForParent(grammarAccess.getDotTransitionRule());
            					}
            					set(
            						current,
            						"layout",
            						lv_layout_5_0,
            						"com.cburch.logisim.statemachine.FSMDSL.LayoutInfo");
            					afterParserOrEnumRuleCall();
            				

            }


            }

            otherlv_6=(Token)match(input,16,FOLLOW_2); 

            			newLeafNode(otherlv_6, grammarAccess.getDotTransitionAccess().getSemicolonKeyword_4());
            		

            }


            }


            	leaveRule();

        }

            catch (RecognitionException re) {
                recover(input,re);
                appendSkippedTokens();
            }
        finally {
        }
        return current;
    }
    // $ANTLR end "ruleDotTransition"


    // $ANTLR start "entryRuleCommand"
    // InternalFSMDSL.g:2387:1: entryRuleCommand returns [EObject current=null] : iv_ruleCommand= ruleCommand EOF ;
    public final EObject entryRuleCommand() throws RecognitionException {
        EObject current = null;

        EObject iv_ruleCommand = null;


        try {
            // InternalFSMDSL.g:2387:48: (iv_ruleCommand= ruleCommand EOF )
            // InternalFSMDSL.g:2388:2: iv_ruleCommand= ruleCommand EOF
            {
             newCompositeNode(grammarAccess.getCommandRule()); 
            pushFollow(FOLLOW_1);
            iv_ruleCommand=ruleCommand();

            state._fsp--;

             current =iv_ruleCommand; 
            match(input,EOF,FOLLOW_2); 

            }

        }

            catch (RecognitionException re) {
                recover(input,re);
                appendSkippedTokens();
            }
        finally {
        }
        return current;
    }
    // $ANTLR end "entryRuleCommand"


    // $ANTLR start "ruleCommand"
    // InternalFSMDSL.g:2394:1: ruleCommand returns [EObject current=null] : ( ( (otherlv_0= RULE_ID ) ) otherlv_1= '=' ( (lv_value_2_0= ruleOr ) ) otherlv_3= ';' ) ;
    public final EObject ruleCommand() throws RecognitionException {
        EObject current = null;

        Token otherlv_0=null;
        Token otherlv_1=null;
        Token otherlv_3=null;
        EObject lv_value_2_0 = null;



        	enterRule();

        try {
            // InternalFSMDSL.g:2400:2: ( ( ( (otherlv_0= RULE_ID ) ) otherlv_1= '=' ( (lv_value_2_0= ruleOr ) ) otherlv_3= ';' ) )
            // InternalFSMDSL.g:2401:2: ( ( (otherlv_0= RULE_ID ) ) otherlv_1= '=' ( (lv_value_2_0= ruleOr ) ) otherlv_3= ';' )
            {
            // InternalFSMDSL.g:2401:2: ( ( (otherlv_0= RULE_ID ) ) otherlv_1= '=' ( (lv_value_2_0= ruleOr ) ) otherlv_3= ';' )
            // InternalFSMDSL.g:2402:3: ( (otherlv_0= RULE_ID ) ) otherlv_1= '=' ( (lv_value_2_0= ruleOr ) ) otherlv_3= ';'
            {
            // InternalFSMDSL.g:2402:3: ( (otherlv_0= RULE_ID ) )
            // InternalFSMDSL.g:2403:4: (otherlv_0= RULE_ID )
            {
            // InternalFSMDSL.g:2403:4: (otherlv_0= RULE_ID )
            // InternalFSMDSL.g:2404:5: otherlv_0= RULE_ID
            {

            					if (current==null) {
            						current = createModelElement(grammarAccess.getCommandRule());
            					}
            				
            otherlv_0=(Token)match(input,RULE_ID,FOLLOW_13); 

            					newLeafNode(otherlv_0, grammarAccess.getCommandAccess().getNameOutputPortCrossReference_0_0());
            				

            }


            }

            otherlv_1=(Token)match(input,18,FOLLOW_12); 

            			newLeafNode(otherlv_1, grammarAccess.getCommandAccess().getEqualsSignKeyword_1());
            		
            // InternalFSMDSL.g:2419:3: ( (lv_value_2_0= ruleOr ) )
            // InternalFSMDSL.g:2420:4: (lv_value_2_0= ruleOr )
            {
            // InternalFSMDSL.g:2420:4: (lv_value_2_0= ruleOr )
            // InternalFSMDSL.g:2421:5: lv_value_2_0= ruleOr
            {

            					newCompositeNode(grammarAccess.getCommandAccess().getValueOrParserRuleCall_2_0());
            				
            pushFollow(FOLLOW_11);
            lv_value_2_0=ruleOr();

            state._fsp--;


            					if (current==null) {
            						current = createModelElementForParent(grammarAccess.getCommandRule());
            					}
            					set(
            						current,
            						"value",
            						lv_value_2_0,
            						"com.cburch.logisim.statemachine.FSMDSL.Or");
            					afterParserOrEnumRuleCall();
            				

            }


            }

            otherlv_3=(Token)match(input,16,FOLLOW_2); 

            			newLeafNode(otherlv_3, grammarAccess.getCommandAccess().getSemicolonKeyword_3());
            		

            }


            }


            	leaveRule();

        }

            catch (RecognitionException re) {
                recover(input,re);
                appendSkippedTokens();
            }
        finally {
        }
        return current;
    }
    // $ANTLR end "ruleCommand"


    // $ANTLR start "entryRuleConcatExpr"
    // InternalFSMDSL.g:2446:1: entryRuleConcatExpr returns [EObject current=null] : iv_ruleConcatExpr= ruleConcatExpr EOF ;
    public final EObject entryRuleConcatExpr() throws RecognitionException {
        EObject current = null;

        EObject iv_ruleConcatExpr = null;


        try {
            // InternalFSMDSL.g:2446:51: (iv_ruleConcatExpr= ruleConcatExpr EOF )
            // InternalFSMDSL.g:2447:2: iv_ruleConcatExpr= ruleConcatExpr EOF
            {
             newCompositeNode(grammarAccess.getConcatExprRule()); 
            pushFollow(FOLLOW_1);
            iv_ruleConcatExpr=ruleConcatExpr();

            state._fsp--;

             current =iv_ruleConcatExpr; 
            match(input,EOF,FOLLOW_2); 

            }

        }

            catch (RecognitionException re) {
                recover(input,re);
                appendSkippedTokens();
            }
        finally {
        }
        return current;
    }
    // $ANTLR end "entryRuleConcatExpr"


    // $ANTLR start "ruleConcatExpr"
    // InternalFSMDSL.g:2453:1: ruleConcatExpr returns [EObject current=null] : ( () otherlv_1= '{' ( (lv_args_2_0= ruleOr ) ) (otherlv_3= ',' ( (lv_args_4_0= ruleOr ) ) )* otherlv_5= '}' ) ;
    public final EObject ruleConcatExpr() throws RecognitionException {
        EObject current = null;

        Token otherlv_1=null;
        Token otherlv_3=null;
        Token otherlv_5=null;
        EObject lv_args_2_0 = null;

        EObject lv_args_4_0 = null;



        	enterRule();

        try {
            // InternalFSMDSL.g:2459:2: ( ( () otherlv_1= '{' ( (lv_args_2_0= ruleOr ) ) (otherlv_3= ',' ( (lv_args_4_0= ruleOr ) ) )* otherlv_5= '}' ) )
            // InternalFSMDSL.g:2460:2: ( () otherlv_1= '{' ( (lv_args_2_0= ruleOr ) ) (otherlv_3= ',' ( (lv_args_4_0= ruleOr ) ) )* otherlv_5= '}' )
            {
            // InternalFSMDSL.g:2460:2: ( () otherlv_1= '{' ( (lv_args_2_0= ruleOr ) ) (otherlv_3= ',' ( (lv_args_4_0= ruleOr ) ) )* otherlv_5= '}' )
            // InternalFSMDSL.g:2461:3: () otherlv_1= '{' ( (lv_args_2_0= ruleOr ) ) (otherlv_3= ',' ( (lv_args_4_0= ruleOr ) ) )* otherlv_5= '}'
            {
            // InternalFSMDSL.g:2461:3: ()
            // InternalFSMDSL.g:2462:4: 
            {

            				current = forceCreateModelElement(
            					grammarAccess.getConcatExprAccess().getConcatExprAction_0(),
            					current);
            			

            }

            otherlv_1=(Token)match(input,27,FOLLOW_12); 

            			newLeafNode(otherlv_1, grammarAccess.getConcatExprAccess().getLeftCurlyBracketKeyword_1());
            		
            // InternalFSMDSL.g:2472:3: ( (lv_args_2_0= ruleOr ) )
            // InternalFSMDSL.g:2473:4: (lv_args_2_0= ruleOr )
            {
            // InternalFSMDSL.g:2473:4: (lv_args_2_0= ruleOr )
            // InternalFSMDSL.g:2474:5: lv_args_2_0= ruleOr
            {

            					newCompositeNode(grammarAccess.getConcatExprAccess().getArgsOrParserRuleCall_2_0());
            				
            pushFollow(FOLLOW_48);
            lv_args_2_0=ruleOr();

            state._fsp--;


            					if (current==null) {
            						current = createModelElementForParent(grammarAccess.getConcatExprRule());
            					}
            					add(
            						current,
            						"args",
            						lv_args_2_0,
            						"com.cburch.logisim.statemachine.FSMDSL.Or");
            					afterParserOrEnumRuleCall();
            				

            }


            }

            // InternalFSMDSL.g:2491:3: (otherlv_3= ',' ( (lv_args_4_0= ruleOr ) ) )*
            loop50:
            do {
                int alt50=2;
                int LA50_0 = input.LA(1);

                if ( (LA50_0==14) ) {
                    alt50=1;
                }


                switch (alt50) {
            	case 1 :
            	    // InternalFSMDSL.g:2492:4: otherlv_3= ',' ( (lv_args_4_0= ruleOr ) )
            	    {
            	    otherlv_3=(Token)match(input,14,FOLLOW_12); 

            	    				newLeafNode(otherlv_3, grammarAccess.getConcatExprAccess().getCommaKeyword_3_0());
            	    			
            	    // InternalFSMDSL.g:2496:4: ( (lv_args_4_0= ruleOr ) )
            	    // InternalFSMDSL.g:2497:5: (lv_args_4_0= ruleOr )
            	    {
            	    // InternalFSMDSL.g:2497:5: (lv_args_4_0= ruleOr )
            	    // InternalFSMDSL.g:2498:6: lv_args_4_0= ruleOr
            	    {

            	    						newCompositeNode(grammarAccess.getConcatExprAccess().getArgsOrParserRuleCall_3_1_0());
            	    					
            	    pushFollow(FOLLOW_48);
            	    lv_args_4_0=ruleOr();

            	    state._fsp--;


            	    						if (current==null) {
            	    							current = createModelElementForParent(grammarAccess.getConcatExprRule());
            	    						}
            	    						add(
            	    							current,
            	    							"args",
            	    							lv_args_4_0,
            	    							"com.cburch.logisim.statemachine.FSMDSL.Or");
            	    						afterParserOrEnumRuleCall();
            	    					

            	    }


            	    }


            	    }
            	    break;

            	default :
            	    break loop50;
                }
            } while (true);

            otherlv_5=(Token)match(input,32,FOLLOW_2); 

            			newLeafNode(otherlv_5, grammarAccess.getConcatExprAccess().getRightCurlyBracketKeyword_4());
            		

            }


            }


            	leaveRule();

        }

            catch (RecognitionException re) {
                recover(input,re);
                appendSkippedTokens();
            }
        finally {
        }
        return current;
    }
    // $ANTLR end "ruleConcatExpr"


    // $ANTLR start "entryRulePortRef"
    // InternalFSMDSL.g:2524:1: entryRulePortRef returns [EObject current=null] : iv_rulePortRef= rulePortRef EOF ;
    public final EObject entryRulePortRef() throws RecognitionException {
        EObject current = null;

        EObject iv_rulePortRef = null;


        try {
            // InternalFSMDSL.g:2524:48: (iv_rulePortRef= rulePortRef EOF )
            // InternalFSMDSL.g:2525:2: iv_rulePortRef= rulePortRef EOF
            {
             newCompositeNode(grammarAccess.getPortRefRule()); 
            pushFollow(FOLLOW_1);
            iv_rulePortRef=rulePortRef();

            state._fsp--;

             current =iv_rulePortRef; 
            match(input,EOF,FOLLOW_2); 

            }

        }

            catch (RecognitionException re) {
                recover(input,re);
                appendSkippedTokens();
            }
        finally {
        }
        return current;
    }
    // $ANTLR end "entryRulePortRef"


    // $ANTLR start "rulePortRef"
    // InternalFSMDSL.g:2531:1: rulePortRef returns [EObject current=null] : ( () ( (otherlv_1= RULE_ID ) ) ( (lv_range_2_0= ruleRange ) )? ) ;
    public final EObject rulePortRef() throws RecognitionException {
        EObject current = null;

        Token otherlv_1=null;
        EObject lv_range_2_0 = null;



        	enterRule();

        try {
            // InternalFSMDSL.g:2537:2: ( ( () ( (otherlv_1= RULE_ID ) ) ( (lv_range_2_0= ruleRange ) )? ) )
            // InternalFSMDSL.g:2538:2: ( () ( (otherlv_1= RULE_ID ) ) ( (lv_range_2_0= ruleRange ) )? )
            {
            // InternalFSMDSL.g:2538:2: ( () ( (otherlv_1= RULE_ID ) ) ( (lv_range_2_0= ruleRange ) )? )
            // InternalFSMDSL.g:2539:3: () ( (otherlv_1= RULE_ID ) ) ( (lv_range_2_0= ruleRange ) )?
            {
            // InternalFSMDSL.g:2539:3: ()
            // InternalFSMDSL.g:2540:4: 
            {

            				current = forceCreateModelElement(
            					grammarAccess.getPortRefAccess().getPortRefAction_0(),
            					current);
            			

            }

            // InternalFSMDSL.g:2546:3: ( (otherlv_1= RULE_ID ) )
            // InternalFSMDSL.g:2547:4: (otherlv_1= RULE_ID )
            {
            // InternalFSMDSL.g:2547:4: (otherlv_1= RULE_ID )
            // InternalFSMDSL.g:2548:5: otherlv_1= RULE_ID
            {

            					if (current==null) {
            						current = createModelElement(grammarAccess.getPortRefRule());
            					}
            				
            otherlv_1=(Token)match(input,RULE_ID,FOLLOW_28); 

            					newLeafNode(otherlv_1, grammarAccess.getPortRefAccess().getPortPortCrossReference_1_0());
            				

            }


            }

            // InternalFSMDSL.g:2559:3: ( (lv_range_2_0= ruleRange ) )?
            int alt51=2;
            int LA51_0 = input.LA(1);

            if ( (LA51_0==13) ) {
                alt51=1;
            }
            switch (alt51) {
                case 1 :
                    // InternalFSMDSL.g:2560:4: (lv_range_2_0= ruleRange )
                    {
                    // InternalFSMDSL.g:2560:4: (lv_range_2_0= ruleRange )
                    // InternalFSMDSL.g:2561:5: lv_range_2_0= ruleRange
                    {

                    					newCompositeNode(grammarAccess.getPortRefAccess().getRangeRangeParserRuleCall_2_0());
                    				
                    pushFollow(FOLLOW_2);
                    lv_range_2_0=ruleRange();

                    state._fsp--;


                    					if (current==null) {
                    						current = createModelElementForParent(grammarAccess.getPortRefRule());
                    					}
                    					set(
                    						current,
                    						"range",
                    						lv_range_2_0,
                    						"com.cburch.logisim.statemachine.FSMDSL.Range");
                    					afterParserOrEnumRuleCall();
                    				

                    }


                    }
                    break;

            }


            }


            }


            	leaveRule();

        }

            catch (RecognitionException re) {
                recover(input,re);
                appendSkippedTokens();
            }
        finally {
        }
        return current;
    }
    // $ANTLR end "rulePortRef"


    // $ANTLR start "entryRuleConstRef"
    // InternalFSMDSL.g:2582:1: entryRuleConstRef returns [EObject current=null] : iv_ruleConstRef= ruleConstRef EOF ;
    public final EObject entryRuleConstRef() throws RecognitionException {
        EObject current = null;

        EObject iv_ruleConstRef = null;


        try {
            // InternalFSMDSL.g:2582:49: (iv_ruleConstRef= ruleConstRef EOF )
            // InternalFSMDSL.g:2583:2: iv_ruleConstRef= ruleConstRef EOF
            {
             newCompositeNode(grammarAccess.getConstRefRule()); 
            pushFollow(FOLLOW_1);
            iv_ruleConstRef=ruleConstRef();

            state._fsp--;

             current =iv_ruleConstRef; 
            match(input,EOF,FOLLOW_2); 

            }

        }

            catch (RecognitionException re) {
                recover(input,re);
                appendSkippedTokens();
            }
        finally {
        }
        return current;
    }
    // $ANTLR end "entryRuleConstRef"


    // $ANTLR start "ruleConstRef"
    // InternalFSMDSL.g:2589:1: ruleConstRef returns [EObject current=null] : ( () otherlv_1= '#' ( (otherlv_2= RULE_ID ) ) ) ;
    public final EObject ruleConstRef() throws RecognitionException {
        EObject current = null;

        Token otherlv_1=null;
        Token otherlv_2=null;


        	enterRule();

        try {
            // InternalFSMDSL.g:2595:2: ( ( () otherlv_1= '#' ( (otherlv_2= RULE_ID ) ) ) )
            // InternalFSMDSL.g:2596:2: ( () otherlv_1= '#' ( (otherlv_2= RULE_ID ) ) )
            {
            // InternalFSMDSL.g:2596:2: ( () otherlv_1= '#' ( (otherlv_2= RULE_ID ) ) )
            // InternalFSMDSL.g:2597:3: () otherlv_1= '#' ( (otherlv_2= RULE_ID ) )
            {
            // InternalFSMDSL.g:2597:3: ()
            // InternalFSMDSL.g:2598:4: 
            {

            				current = forceCreateModelElement(
            					grammarAccess.getConstRefAccess().getConstRefAction_0(),
            					current);
            			

            }

            otherlv_1=(Token)match(input,42,FOLLOW_8); 

            			newLeafNode(otherlv_1, grammarAccess.getConstRefAccess().getNumberSignKeyword_1());
            		
            // InternalFSMDSL.g:2608:3: ( (otherlv_2= RULE_ID ) )
            // InternalFSMDSL.g:2609:4: (otherlv_2= RULE_ID )
            {
            // InternalFSMDSL.g:2609:4: (otherlv_2= RULE_ID )
            // InternalFSMDSL.g:2610:5: otherlv_2= RULE_ID
            {

            					if (current==null) {
            						current = createModelElement(grammarAccess.getConstRefRule());
            					}
            				
            otherlv_2=(Token)match(input,RULE_ID,FOLLOW_2); 

            					newLeafNode(otherlv_2, grammarAccess.getConstRefAccess().getConstConstantDefCrossReference_2_0());
            				

            }


            }


            }


            }


            	leaveRule();

        }

            catch (RecognitionException re) {
                recover(input,re);
                appendSkippedTokens();
            }
        finally {
        }
        return current;
    }
    // $ANTLR end "ruleConstRef"


    // $ANTLR start "entryRuleRange"
    // InternalFSMDSL.g:2625:1: entryRuleRange returns [EObject current=null] : iv_ruleRange= ruleRange EOF ;
    public final EObject entryRuleRange() throws RecognitionException {
        EObject current = null;

        EObject iv_ruleRange = null;


        try {
            // InternalFSMDSL.g:2625:46: (iv_ruleRange= ruleRange EOF )
            // InternalFSMDSL.g:2626:2: iv_ruleRange= ruleRange EOF
            {
             newCompositeNode(grammarAccess.getRangeRule()); 
            pushFollow(FOLLOW_1);
            iv_ruleRange=ruleRange();

            state._fsp--;

             current =iv_ruleRange; 
            match(input,EOF,FOLLOW_2); 

            }

        }

            catch (RecognitionException re) {
                recover(input,re);
                appendSkippedTokens();
            }
        finally {
        }
        return current;
    }
    // $ANTLR end "entryRuleRange"


    // $ANTLR start "ruleRange"
    // InternalFSMDSL.g:2632:1: ruleRange returns [EObject current=null] : (otherlv_0= '[' ( ( (lv_ub_1_0= RULE_INT ) ) otherlv_2= ':' )? ( (lv_lb_3_0= RULE_INT ) ) otherlv_4= ']' ) ;
    public final EObject ruleRange() throws RecognitionException {
        EObject current = null;

        Token otherlv_0=null;
        Token lv_ub_1_0=null;
        Token otherlv_2=null;
        Token lv_lb_3_0=null;
        Token otherlv_4=null;


        	enterRule();

        try {
            // InternalFSMDSL.g:2638:2: ( (otherlv_0= '[' ( ( (lv_ub_1_0= RULE_INT ) ) otherlv_2= ':' )? ( (lv_lb_3_0= RULE_INT ) ) otherlv_4= ']' ) )
            // InternalFSMDSL.g:2639:2: (otherlv_0= '[' ( ( (lv_ub_1_0= RULE_INT ) ) otherlv_2= ':' )? ( (lv_lb_3_0= RULE_INT ) ) otherlv_4= ']' )
            {
            // InternalFSMDSL.g:2639:2: (otherlv_0= '[' ( ( (lv_ub_1_0= RULE_INT ) ) otherlv_2= ':' )? ( (lv_lb_3_0= RULE_INT ) ) otherlv_4= ']' )
            // InternalFSMDSL.g:2640:3: otherlv_0= '[' ( ( (lv_ub_1_0= RULE_INT ) ) otherlv_2= ':' )? ( (lv_lb_3_0= RULE_INT ) ) otherlv_4= ']'
            {
            otherlv_0=(Token)match(input,13,FOLLOW_25); 

            			newLeafNode(otherlv_0, grammarAccess.getRangeAccess().getLeftSquareBracketKeyword_0());
            		
            // InternalFSMDSL.g:2644:3: ( ( (lv_ub_1_0= RULE_INT ) ) otherlv_2= ':' )?
            int alt52=2;
            int LA52_0 = input.LA(1);

            if ( (LA52_0==RULE_INT) ) {
                int LA52_1 = input.LA(2);

                if ( (LA52_1==37) ) {
                    alt52=1;
                }
            }
            switch (alt52) {
                case 1 :
                    // InternalFSMDSL.g:2645:4: ( (lv_ub_1_0= RULE_INT ) ) otherlv_2= ':'
                    {
                    // InternalFSMDSL.g:2645:4: ( (lv_ub_1_0= RULE_INT ) )
                    // InternalFSMDSL.g:2646:5: (lv_ub_1_0= RULE_INT )
                    {
                    // InternalFSMDSL.g:2646:5: (lv_ub_1_0= RULE_INT )
                    // InternalFSMDSL.g:2647:6: lv_ub_1_0= RULE_INT
                    {
                    lv_ub_1_0=(Token)match(input,RULE_INT,FOLLOW_41); 

                    						newLeafNode(lv_ub_1_0, grammarAccess.getRangeAccess().getUbINTTerminalRuleCall_1_0_0());
                    					

                    						if (current==null) {
                    							current = createModelElement(grammarAccess.getRangeRule());
                    						}
                    						setWithLastConsumed(
                    							current,
                    							"ub",
                    							lv_ub_1_0,
                    							"org.eclipse.xtext.common.Terminals.INT");
                    					

                    }


                    }

                    otherlv_2=(Token)match(input,37,FOLLOW_25); 

                    				newLeafNode(otherlv_2, grammarAccess.getRangeAccess().getColonKeyword_1_1());
                    			

                    }
                    break;

            }

            // InternalFSMDSL.g:2668:3: ( (lv_lb_3_0= RULE_INT ) )
            // InternalFSMDSL.g:2669:4: (lv_lb_3_0= RULE_INT )
            {
            // InternalFSMDSL.g:2669:4: (lv_lb_3_0= RULE_INT )
            // InternalFSMDSL.g:2670:5: lv_lb_3_0= RULE_INT
            {
            lv_lb_3_0=(Token)match(input,RULE_INT,FOLLOW_29); 

            					newLeafNode(lv_lb_3_0, grammarAccess.getRangeAccess().getLbINTTerminalRuleCall_2_0());
            				

            					if (current==null) {
            						current = createModelElement(grammarAccess.getRangeRule());
            					}
            					setWithLastConsumed(
            						current,
            						"lb",
            						lv_lb_3_0,
            						"org.eclipse.xtext.common.Terminals.INT");
            				

            }


            }

            otherlv_4=(Token)match(input,15,FOLLOW_2); 

            			newLeafNode(otherlv_4, grammarAccess.getRangeAccess().getRightSquareBracketKeyword_3());
            		

            }


            }


            	leaveRule();

        }

            catch (RecognitionException re) {
                recover(input,re);
                appendSkippedTokens();
            }
        finally {
        }
        return current;
    }
    // $ANTLR end "ruleRange"


    // $ANTLR start "entryRulePredicate"
    // InternalFSMDSL.g:2694:1: entryRulePredicate returns [EObject current=null] : iv_rulePredicate= rulePredicate EOF ;
    public final EObject entryRulePredicate() throws RecognitionException {
        EObject current = null;

        EObject iv_rulePredicate = null;


        try {
            // InternalFSMDSL.g:2694:50: (iv_rulePredicate= rulePredicate EOF )
            // InternalFSMDSL.g:2695:2: iv_rulePredicate= rulePredicate EOF
            {
             newCompositeNode(grammarAccess.getPredicateRule()); 
            pushFollow(FOLLOW_1);
            iv_rulePredicate=rulePredicate();

            state._fsp--;

             current =iv_rulePredicate; 
            match(input,EOF,FOLLOW_2); 

            }

        }

            catch (RecognitionException re) {
                recover(input,re);
                appendSkippedTokens();
            }
        finally {
        }
        return current;
    }
    // $ANTLR end "entryRulePredicate"


    // $ANTLR start "rulePredicate"
    // InternalFSMDSL.g:2701:1: rulePredicate returns [EObject current=null] : (this_Default_0= ruleDefault | this_Or_1= ruleOr ) ;
    public final EObject rulePredicate() throws RecognitionException {
        EObject current = null;

        EObject this_Default_0 = null;

        EObject this_Or_1 = null;



        	enterRule();

        try {
            // InternalFSMDSL.g:2707:2: ( (this_Default_0= ruleDefault | this_Or_1= ruleOr ) )
            // InternalFSMDSL.g:2708:2: (this_Default_0= ruleDefault | this_Or_1= ruleOr )
            {
            // InternalFSMDSL.g:2708:2: (this_Default_0= ruleDefault | this_Or_1= ruleOr )
            int alt53=2;
            int LA53_0 = input.LA(1);

            if ( (LA53_0==43) ) {
                alt53=1;
            }
            else if ( (LA53_0==RULE_ID||(LA53_0>=RULE_BIN && LA53_0<=RULE_HEX)||LA53_0==22||LA53_0==27||LA53_0==42||LA53_0==47) ) {
                alt53=2;
            }
            else {
                NoViableAltException nvae =
                    new NoViableAltException("", 53, 0, input);

                throw nvae;
            }
            switch (alt53) {
                case 1 :
                    // InternalFSMDSL.g:2709:3: this_Default_0= ruleDefault
                    {

                    			newCompositeNode(grammarAccess.getPredicateAccess().getDefaultParserRuleCall_0());
                    		
                    pushFollow(FOLLOW_2);
                    this_Default_0=ruleDefault();

                    state._fsp--;


                    			current = this_Default_0;
                    			afterParserOrEnumRuleCall();
                    		

                    }
                    break;
                case 2 :
                    // InternalFSMDSL.g:2718:3: this_Or_1= ruleOr
                    {

                    			newCompositeNode(grammarAccess.getPredicateAccess().getOrParserRuleCall_1());
                    		
                    pushFollow(FOLLOW_2);
                    this_Or_1=ruleOr();

                    state._fsp--;


                    			current = this_Or_1;
                    			afterParserOrEnumRuleCall();
                    		

                    }
                    break;

            }


            }


            	leaveRule();

        }

            catch (RecognitionException re) {
                recover(input,re);
                appendSkippedTokens();
            }
        finally {
        }
        return current;
    }
    // $ANTLR end "rulePredicate"


    // $ANTLR start "entryRuleDefault"
    // InternalFSMDSL.g:2730:1: entryRuleDefault returns [EObject current=null] : iv_ruleDefault= ruleDefault EOF ;
    public final EObject entryRuleDefault() throws RecognitionException {
        EObject current = null;

        EObject iv_ruleDefault = null;


        try {
            // InternalFSMDSL.g:2730:48: (iv_ruleDefault= ruleDefault EOF )
            // InternalFSMDSL.g:2731:2: iv_ruleDefault= ruleDefault EOF
            {
             newCompositeNode(grammarAccess.getDefaultRule()); 
            pushFollow(FOLLOW_1);
            iv_ruleDefault=ruleDefault();

            state._fsp--;

             current =iv_ruleDefault; 
            match(input,EOF,FOLLOW_2); 

            }

        }

            catch (RecognitionException re) {
                recover(input,re);
                appendSkippedTokens();
            }
        finally {
        }
        return current;
    }
    // $ANTLR end "entryRuleDefault"


    // $ANTLR start "ruleDefault"
    // InternalFSMDSL.g:2737:1: ruleDefault returns [EObject current=null] : ( () otherlv_1= 'default' ) ;
    public final EObject ruleDefault() throws RecognitionException {
        EObject current = null;

        Token otherlv_1=null;


        	enterRule();

        try {
            // InternalFSMDSL.g:2743:2: ( ( () otherlv_1= 'default' ) )
            // InternalFSMDSL.g:2744:2: ( () otherlv_1= 'default' )
            {
            // InternalFSMDSL.g:2744:2: ( () otherlv_1= 'default' )
            // InternalFSMDSL.g:2745:3: () otherlv_1= 'default'
            {
            // InternalFSMDSL.g:2745:3: ()
            // InternalFSMDSL.g:2746:4: 
            {

            				current = forceCreateModelElement(
            					grammarAccess.getDefaultAccess().getDefaultPredicateAction_0(),
            					current);
            			

            }

            otherlv_1=(Token)match(input,43,FOLLOW_2); 

            			newLeafNode(otherlv_1, grammarAccess.getDefaultAccess().getDefaultKeyword_1());
            		

            }


            }


            	leaveRule();

        }

            catch (RecognitionException re) {
                recover(input,re);
                appendSkippedTokens();
            }
        finally {
        }
        return current;
    }
    // $ANTLR end "ruleDefault"


    // $ANTLR start "entryRuleOr"
    // InternalFSMDSL.g:2760:1: entryRuleOr returns [EObject current=null] : iv_ruleOr= ruleOr EOF ;
    public final EObject entryRuleOr() throws RecognitionException {
        EObject current = null;

        EObject iv_ruleOr = null;


        try {
            // InternalFSMDSL.g:2760:43: (iv_ruleOr= ruleOr EOF )
            // InternalFSMDSL.g:2761:2: iv_ruleOr= ruleOr EOF
            {
             newCompositeNode(grammarAccess.getOrRule()); 
            pushFollow(FOLLOW_1);
            iv_ruleOr=ruleOr();

            state._fsp--;

             current =iv_ruleOr; 
            match(input,EOF,FOLLOW_2); 

            }

        }

            catch (RecognitionException re) {
                recover(input,re);
                appendSkippedTokens();
            }
        finally {
        }
        return current;
    }
    // $ANTLR end "entryRuleOr"


    // $ANTLR start "ruleOr"
    // InternalFSMDSL.g:2767:1: ruleOr returns [EObject current=null] : (this_And_0= ruleAnd ( () otherlv_2= '+' ( (lv_args_3_0= ruleAnd ) ) )* ) ;
    public final EObject ruleOr() throws RecognitionException {
        EObject current = null;

        Token otherlv_2=null;
        EObject this_And_0 = null;

        EObject lv_args_3_0 = null;



        	enterRule();

        try {
            // InternalFSMDSL.g:2773:2: ( (this_And_0= ruleAnd ( () otherlv_2= '+' ( (lv_args_3_0= ruleAnd ) ) )* ) )
            // InternalFSMDSL.g:2774:2: (this_And_0= ruleAnd ( () otherlv_2= '+' ( (lv_args_3_0= ruleAnd ) ) )* )
            {
            // InternalFSMDSL.g:2774:2: (this_And_0= ruleAnd ( () otherlv_2= '+' ( (lv_args_3_0= ruleAnd ) ) )* )
            // InternalFSMDSL.g:2775:3: this_And_0= ruleAnd ( () otherlv_2= '+' ( (lv_args_3_0= ruleAnd ) ) )*
            {

            			newCompositeNode(grammarAccess.getOrAccess().getAndParserRuleCall_0());
            		
            pushFollow(FOLLOW_18);
            this_And_0=ruleAnd();

            state._fsp--;


            			current = this_And_0;
            			afterParserOrEnumRuleCall();
            		
            // InternalFSMDSL.g:2783:3: ( () otherlv_2= '+' ( (lv_args_3_0= ruleAnd ) ) )*
            loop54:
            do {
                int alt54=2;
                int LA54_0 = input.LA(1);

                if ( (LA54_0==20) ) {
                    alt54=1;
                }


                switch (alt54) {
            	case 1 :
            	    // InternalFSMDSL.g:2784:4: () otherlv_2= '+' ( (lv_args_3_0= ruleAnd ) )
            	    {
            	    // InternalFSMDSL.g:2784:4: ()
            	    // InternalFSMDSL.g:2785:5: 
            	    {

            	    					current = forceCreateModelElementAndAdd(
            	    						grammarAccess.getOrAccess().getOrExprArgsAction_1_0(),
            	    						current);
            	    				

            	    }

            	    otherlv_2=(Token)match(input,20,FOLLOW_12); 

            	    				newLeafNode(otherlv_2, grammarAccess.getOrAccess().getPlusSignKeyword_1_1());
            	    			
            	    // InternalFSMDSL.g:2795:4: ( (lv_args_3_0= ruleAnd ) )
            	    // InternalFSMDSL.g:2796:5: (lv_args_3_0= ruleAnd )
            	    {
            	    // InternalFSMDSL.g:2796:5: (lv_args_3_0= ruleAnd )
            	    // InternalFSMDSL.g:2797:6: lv_args_3_0= ruleAnd
            	    {

            	    						newCompositeNode(grammarAccess.getOrAccess().getArgsAndParserRuleCall_1_2_0());
            	    					
            	    pushFollow(FOLLOW_18);
            	    lv_args_3_0=ruleAnd();

            	    state._fsp--;


            	    						if (current==null) {
            	    							current = createModelElementForParent(grammarAccess.getOrRule());
            	    						}
            	    						add(
            	    							current,
            	    							"args",
            	    							lv_args_3_0,
            	    							"com.cburch.logisim.statemachine.FSMDSL.And");
            	    						afterParserOrEnumRuleCall();
            	    					

            	    }


            	    }


            	    }
            	    break;

            	default :
            	    break loop54;
                }
            } while (true);


            }


            }


            	leaveRule();

        }

            catch (RecognitionException re) {
                recover(input,re);
                appendSkippedTokens();
            }
        finally {
        }
        return current;
    }
    // $ANTLR end "ruleOr"


    // $ANTLR start "entryRuleAnd"
    // InternalFSMDSL.g:2819:1: entryRuleAnd returns [EObject current=null] : iv_ruleAnd= ruleAnd EOF ;
    public final EObject entryRuleAnd() throws RecognitionException {
        EObject current = null;

        EObject iv_ruleAnd = null;


        try {
            // InternalFSMDSL.g:2819:44: (iv_ruleAnd= ruleAnd EOF )
            // InternalFSMDSL.g:2820:2: iv_ruleAnd= ruleAnd EOF
            {
             newCompositeNode(grammarAccess.getAndRule()); 
            pushFollow(FOLLOW_1);
            iv_ruleAnd=ruleAnd();

            state._fsp--;

             current =iv_ruleAnd; 
            match(input,EOF,FOLLOW_2); 

            }

        }

            catch (RecognitionException re) {
                recover(input,re);
                appendSkippedTokens();
            }
        finally {
        }
        return current;
    }
    // $ANTLR end "entryRuleAnd"


    // $ANTLR start "ruleAnd"
    // InternalFSMDSL.g:2826:1: ruleAnd returns [EObject current=null] : (this_Cmp_0= ruleCmp ( () otherlv_2= '.' ( (lv_args_3_0= ruleCmp ) ) )* ) ;
    public final EObject ruleAnd() throws RecognitionException {
        EObject current = null;

        Token otherlv_2=null;
        EObject this_Cmp_0 = null;

        EObject lv_args_3_0 = null;



        	enterRule();

        try {
            // InternalFSMDSL.g:2832:2: ( (this_Cmp_0= ruleCmp ( () otherlv_2= '.' ( (lv_args_3_0= ruleCmp ) ) )* ) )
            // InternalFSMDSL.g:2833:2: (this_Cmp_0= ruleCmp ( () otherlv_2= '.' ( (lv_args_3_0= ruleCmp ) ) )* )
            {
            // InternalFSMDSL.g:2833:2: (this_Cmp_0= ruleCmp ( () otherlv_2= '.' ( (lv_args_3_0= ruleCmp ) ) )* )
            // InternalFSMDSL.g:2834:3: this_Cmp_0= ruleCmp ( () otherlv_2= '.' ( (lv_args_3_0= ruleCmp ) ) )*
            {

            			newCompositeNode(grammarAccess.getAndAccess().getCmpParserRuleCall_0());
            		
            pushFollow(FOLLOW_49);
            this_Cmp_0=ruleCmp();

            state._fsp--;


            			current = this_Cmp_0;
            			afterParserOrEnumRuleCall();
            		
            // InternalFSMDSL.g:2842:3: ( () otherlv_2= '.' ( (lv_args_3_0= ruleCmp ) ) )*
            loop55:
            do {
                int alt55=2;
                int LA55_0 = input.LA(1);

                if ( (LA55_0==44) ) {
                    alt55=1;
                }


                switch (alt55) {
            	case 1 :
            	    // InternalFSMDSL.g:2843:4: () otherlv_2= '.' ( (lv_args_3_0= ruleCmp ) )
            	    {
            	    // InternalFSMDSL.g:2843:4: ()
            	    // InternalFSMDSL.g:2844:5: 
            	    {

            	    					current = forceCreateModelElementAndAdd(
            	    						grammarAccess.getAndAccess().getAndExprArgsAction_1_0(),
            	    						current);
            	    				

            	    }

            	    otherlv_2=(Token)match(input,44,FOLLOW_12); 

            	    				newLeafNode(otherlv_2, grammarAccess.getAndAccess().getFullStopKeyword_1_1());
            	    			
            	    // InternalFSMDSL.g:2854:4: ( (lv_args_3_0= ruleCmp ) )
            	    // InternalFSMDSL.g:2855:5: (lv_args_3_0= ruleCmp )
            	    {
            	    // InternalFSMDSL.g:2855:5: (lv_args_3_0= ruleCmp )
            	    // InternalFSMDSL.g:2856:6: lv_args_3_0= ruleCmp
            	    {

            	    						newCompositeNode(grammarAccess.getAndAccess().getArgsCmpParserRuleCall_1_2_0());
            	    					
            	    pushFollow(FOLLOW_49);
            	    lv_args_3_0=ruleCmp();

            	    state._fsp--;


            	    						if (current==null) {
            	    							current = createModelElementForParent(grammarAccess.getAndRule());
            	    						}
            	    						add(
            	    							current,
            	    							"args",
            	    							lv_args_3_0,
            	    							"com.cburch.logisim.statemachine.FSMDSL.Cmp");
            	    						afterParserOrEnumRuleCall();
            	    					

            	    }


            	    }


            	    }
            	    break;

            	default :
            	    break loop55;
                }
            } while (true);


            }


            }


            	leaveRule();

        }

            catch (RecognitionException re) {
                recover(input,re);
                appendSkippedTokens();
            }
        finally {
        }
        return current;
    }
    // $ANTLR end "ruleAnd"


    // $ANTLR start "entryRuleCmp"
    // InternalFSMDSL.g:2878:1: entryRuleCmp returns [EObject current=null] : iv_ruleCmp= ruleCmp EOF ;
    public final EObject entryRuleCmp() throws RecognitionException {
        EObject current = null;

        EObject iv_ruleCmp = null;


        try {
            // InternalFSMDSL.g:2878:44: (iv_ruleCmp= ruleCmp EOF )
            // InternalFSMDSL.g:2879:2: iv_ruleCmp= ruleCmp EOF
            {
             newCompositeNode(grammarAccess.getCmpRule()); 
            pushFollow(FOLLOW_1);
            iv_ruleCmp=ruleCmp();

            state._fsp--;

             current =iv_ruleCmp; 
            match(input,EOF,FOLLOW_2); 

            }

        }

            catch (RecognitionException re) {
                recover(input,re);
                appendSkippedTokens();
            }
        finally {
        }
        return current;
    }
    // $ANTLR end "entryRuleCmp"


    // $ANTLR start "ruleCmp"
    // InternalFSMDSL.g:2885:1: ruleCmp returns [EObject current=null] : (this_Primary_0= rulePrimary ( () ( ( (lv_op_2_1= '==' | lv_op_2_2= '/=' ) ) ) ( (lv_args_3_0= rulePrimary ) ) )? ) ;
    public final EObject ruleCmp() throws RecognitionException {
        EObject current = null;

        Token lv_op_2_1=null;
        Token lv_op_2_2=null;
        EObject this_Primary_0 = null;

        EObject lv_args_3_0 = null;



        	enterRule();

        try {
            // InternalFSMDSL.g:2891:2: ( (this_Primary_0= rulePrimary ( () ( ( (lv_op_2_1= '==' | lv_op_2_2= '/=' ) ) ) ( (lv_args_3_0= rulePrimary ) ) )? ) )
            // InternalFSMDSL.g:2892:2: (this_Primary_0= rulePrimary ( () ( ( (lv_op_2_1= '==' | lv_op_2_2= '/=' ) ) ) ( (lv_args_3_0= rulePrimary ) ) )? )
            {
            // InternalFSMDSL.g:2892:2: (this_Primary_0= rulePrimary ( () ( ( (lv_op_2_1= '==' | lv_op_2_2= '/=' ) ) ) ( (lv_args_3_0= rulePrimary ) ) )? )
            // InternalFSMDSL.g:2893:3: this_Primary_0= rulePrimary ( () ( ( (lv_op_2_1= '==' | lv_op_2_2= '/=' ) ) ) ( (lv_args_3_0= rulePrimary ) ) )?
            {

            			newCompositeNode(grammarAccess.getCmpAccess().getPrimaryParserRuleCall_0());
            		
            pushFollow(FOLLOW_50);
            this_Primary_0=rulePrimary();

            state._fsp--;


            			current = this_Primary_0;
            			afterParserOrEnumRuleCall();
            		
            // InternalFSMDSL.g:2901:3: ( () ( ( (lv_op_2_1= '==' | lv_op_2_2= '/=' ) ) ) ( (lv_args_3_0= rulePrimary ) ) )?
            int alt57=2;
            int LA57_0 = input.LA(1);

            if ( ((LA57_0>=45 && LA57_0<=46)) ) {
                alt57=1;
            }
            switch (alt57) {
                case 1 :
                    // InternalFSMDSL.g:2902:4: () ( ( (lv_op_2_1= '==' | lv_op_2_2= '/=' ) ) ) ( (lv_args_3_0= rulePrimary ) )
                    {
                    // InternalFSMDSL.g:2902:4: ()
                    // InternalFSMDSL.g:2903:5: 
                    {

                    					current = forceCreateModelElementAndAdd(
                    						grammarAccess.getCmpAccess().getCmpExprArgsAction_1_0(),
                    						current);
                    				

                    }

                    // InternalFSMDSL.g:2909:4: ( ( (lv_op_2_1= '==' | lv_op_2_2= '/=' ) ) )
                    // InternalFSMDSL.g:2910:5: ( (lv_op_2_1= '==' | lv_op_2_2= '/=' ) )
                    {
                    // InternalFSMDSL.g:2910:5: ( (lv_op_2_1= '==' | lv_op_2_2= '/=' ) )
                    // InternalFSMDSL.g:2911:6: (lv_op_2_1= '==' | lv_op_2_2= '/=' )
                    {
                    // InternalFSMDSL.g:2911:6: (lv_op_2_1= '==' | lv_op_2_2= '/=' )
                    int alt56=2;
                    int LA56_0 = input.LA(1);

                    if ( (LA56_0==45) ) {
                        alt56=1;
                    }
                    else if ( (LA56_0==46) ) {
                        alt56=2;
                    }
                    else {
                        NoViableAltException nvae =
                            new NoViableAltException("", 56, 0, input);

                        throw nvae;
                    }
                    switch (alt56) {
                        case 1 :
                            // InternalFSMDSL.g:2912:7: lv_op_2_1= '=='
                            {
                            lv_op_2_1=(Token)match(input,45,FOLLOW_12); 

                            							newLeafNode(lv_op_2_1, grammarAccess.getCmpAccess().getOpEqualsSignEqualsSignKeyword_1_1_0_0());
                            						

                            							if (current==null) {
                            								current = createModelElement(grammarAccess.getCmpRule());
                            							}
                            							setWithLastConsumed(current, "op", lv_op_2_1, null);
                            						

                            }
                            break;
                        case 2 :
                            // InternalFSMDSL.g:2923:7: lv_op_2_2= '/='
                            {
                            lv_op_2_2=(Token)match(input,46,FOLLOW_12); 

                            							newLeafNode(lv_op_2_2, grammarAccess.getCmpAccess().getOpSolidusEqualsSignKeyword_1_1_0_1());
                            						

                            							if (current==null) {
                            								current = createModelElement(grammarAccess.getCmpRule());
                            							}
                            							setWithLastConsumed(current, "op", lv_op_2_2, null);
                            						

                            }
                            break;

                    }


                    }


                    }

                    // InternalFSMDSL.g:2936:4: ( (lv_args_3_0= rulePrimary ) )
                    // InternalFSMDSL.g:2937:5: (lv_args_3_0= rulePrimary )
                    {
                    // InternalFSMDSL.g:2937:5: (lv_args_3_0= rulePrimary )
                    // InternalFSMDSL.g:2938:6: lv_args_3_0= rulePrimary
                    {

                    						newCompositeNode(grammarAccess.getCmpAccess().getArgsPrimaryParserRuleCall_1_2_0());
                    					
                    pushFollow(FOLLOW_2);
                    lv_args_3_0=rulePrimary();

                    state._fsp--;


                    						if (current==null) {
                    							current = createModelElementForParent(grammarAccess.getCmpRule());
                    						}
                    						add(
                    							current,
                    							"args",
                    							lv_args_3_0,
                    							"com.cburch.logisim.statemachine.FSMDSL.Primary");
                    						afterParserOrEnumRuleCall();
                    					

                    }


                    }


                    }
                    break;

            }


            }


            }


            	leaveRule();

        }

            catch (RecognitionException re) {
                recover(input,re);
                appendSkippedTokens();
            }
        finally {
        }
        return current;
    }
    // $ANTLR end "ruleCmp"


    // $ANTLR start "entryRulePrimary"
    // InternalFSMDSL.g:2960:1: entryRulePrimary returns [EObject current=null] : iv_rulePrimary= rulePrimary EOF ;
    public final EObject entryRulePrimary() throws RecognitionException {
        EObject current = null;

        EObject iv_rulePrimary = null;


        try {
            // InternalFSMDSL.g:2960:48: (iv_rulePrimary= rulePrimary EOF )
            // InternalFSMDSL.g:2961:2: iv_rulePrimary= rulePrimary EOF
            {
             newCompositeNode(grammarAccess.getPrimaryRule()); 
            pushFollow(FOLLOW_1);
            iv_rulePrimary=rulePrimary();

            state._fsp--;

             current =iv_rulePrimary; 
            match(input,EOF,FOLLOW_2); 

            }

        }

            catch (RecognitionException re) {
                recover(input,re);
                appendSkippedTokens();
            }
        finally {
        }
        return current;
    }
    // $ANTLR end "entryRulePrimary"


    // $ANTLR start "rulePrimary"
    // InternalFSMDSL.g:2967:1: rulePrimary returns [EObject current=null] : ( (otherlv_0= '(' this_Or_1= ruleOr otherlv_2= ')' ) | this_Constant_3= ruleConstant | this_Not_4= ruleNot | this_ConcatExpr_5= ruleConcatExpr | this_PortRef_6= rulePortRef | this_ConstRef_7= ruleConstRef ) ;
    public final EObject rulePrimary() throws RecognitionException {
        EObject current = null;

        Token otherlv_0=null;
        Token otherlv_2=null;
        EObject this_Or_1 = null;

        EObject this_Constant_3 = null;

        EObject this_Not_4 = null;

        EObject this_ConcatExpr_5 = null;

        EObject this_PortRef_6 = null;

        EObject this_ConstRef_7 = null;



        	enterRule();

        try {
            // InternalFSMDSL.g:2973:2: ( ( (otherlv_0= '(' this_Or_1= ruleOr otherlv_2= ')' ) | this_Constant_3= ruleConstant | this_Not_4= ruleNot | this_ConcatExpr_5= ruleConcatExpr | this_PortRef_6= rulePortRef | this_ConstRef_7= ruleConstRef ) )
            // InternalFSMDSL.g:2974:2: ( (otherlv_0= '(' this_Or_1= ruleOr otherlv_2= ')' ) | this_Constant_3= ruleConstant | this_Not_4= ruleNot | this_ConcatExpr_5= ruleConcatExpr | this_PortRef_6= rulePortRef | this_ConstRef_7= ruleConstRef )
            {
            // InternalFSMDSL.g:2974:2: ( (otherlv_0= '(' this_Or_1= ruleOr otherlv_2= ')' ) | this_Constant_3= ruleConstant | this_Not_4= ruleNot | this_ConcatExpr_5= ruleConcatExpr | this_PortRef_6= rulePortRef | this_ConstRef_7= ruleConstRef )
            int alt58=6;
            switch ( input.LA(1) ) {
            case 22:
                {
                alt58=1;
                }
                break;
            case RULE_BIN:
            case RULE_HEX:
                {
                alt58=2;
                }
                break;
            case 47:
                {
                alt58=3;
                }
                break;
            case 27:
                {
                alt58=4;
                }
                break;
            case RULE_ID:
                {
                alt58=5;
                }
                break;
            case 42:
                {
                alt58=6;
                }
                break;
            default:
                NoViableAltException nvae =
                    new NoViableAltException("", 58, 0, input);

                throw nvae;
            }

            switch (alt58) {
                case 1 :
                    // InternalFSMDSL.g:2975:3: (otherlv_0= '(' this_Or_1= ruleOr otherlv_2= ')' )
                    {
                    // InternalFSMDSL.g:2975:3: (otherlv_0= '(' this_Or_1= ruleOr otherlv_2= ')' )
                    // InternalFSMDSL.g:2976:4: otherlv_0= '(' this_Or_1= ruleOr otherlv_2= ')'
                    {
                    otherlv_0=(Token)match(input,22,FOLLOW_12); 

                    				newLeafNode(otherlv_0, grammarAccess.getPrimaryAccess().getLeftParenthesisKeyword_0_0());
                    			

                    				newCompositeNode(grammarAccess.getPrimaryAccess().getOrParserRuleCall_0_1());
                    			
                    pushFollow(FOLLOW_20);
                    this_Or_1=ruleOr();

                    state._fsp--;


                    				current = this_Or_1;
                    				afterParserOrEnumRuleCall();
                    			
                    otherlv_2=(Token)match(input,23,FOLLOW_2); 

                    				newLeafNode(otherlv_2, grammarAccess.getPrimaryAccess().getRightParenthesisKeyword_0_2());
                    			

                    }


                    }
                    break;
                case 2 :
                    // InternalFSMDSL.g:2994:3: this_Constant_3= ruleConstant
                    {

                    			newCompositeNode(grammarAccess.getPrimaryAccess().getConstantParserRuleCall_1());
                    		
                    pushFollow(FOLLOW_2);
                    this_Constant_3=ruleConstant();

                    state._fsp--;


                    			current = this_Constant_3;
                    			afterParserOrEnumRuleCall();
                    		

                    }
                    break;
                case 3 :
                    // InternalFSMDSL.g:3003:3: this_Not_4= ruleNot
                    {

                    			newCompositeNode(grammarAccess.getPrimaryAccess().getNotParserRuleCall_2());
                    		
                    pushFollow(FOLLOW_2);
                    this_Not_4=ruleNot();

                    state._fsp--;


                    			current = this_Not_4;
                    			afterParserOrEnumRuleCall();
                    		

                    }
                    break;
                case 4 :
                    // InternalFSMDSL.g:3012:3: this_ConcatExpr_5= ruleConcatExpr
                    {

                    			newCompositeNode(grammarAccess.getPrimaryAccess().getConcatExprParserRuleCall_3());
                    		
                    pushFollow(FOLLOW_2);
                    this_ConcatExpr_5=ruleConcatExpr();

                    state._fsp--;


                    			current = this_ConcatExpr_5;
                    			afterParserOrEnumRuleCall();
                    		

                    }
                    break;
                case 5 :
                    // InternalFSMDSL.g:3021:3: this_PortRef_6= rulePortRef
                    {

                    			newCompositeNode(grammarAccess.getPrimaryAccess().getPortRefParserRuleCall_4());
                    		
                    pushFollow(FOLLOW_2);
                    this_PortRef_6=rulePortRef();

                    state._fsp--;


                    			current = this_PortRef_6;
                    			afterParserOrEnumRuleCall();
                    		

                    }
                    break;
                case 6 :
                    // InternalFSMDSL.g:3030:3: this_ConstRef_7= ruleConstRef
                    {

                    			newCompositeNode(grammarAccess.getPrimaryAccess().getConstRefParserRuleCall_5());
                    		
                    pushFollow(FOLLOW_2);
                    this_ConstRef_7=ruleConstRef();

                    state._fsp--;


                    			current = this_ConstRef_7;
                    			afterParserOrEnumRuleCall();
                    		

                    }
                    break;

            }


            }


            	leaveRule();

        }

            catch (RecognitionException re) {
                recover(input,re);
                appendSkippedTokens();
            }
        finally {
        }
        return current;
    }
    // $ANTLR end "rulePrimary"


    // $ANTLR start "entryRuleNot"
    // InternalFSMDSL.g:3042:1: entryRuleNot returns [EObject current=null] : iv_ruleNot= ruleNot EOF ;
    public final EObject entryRuleNot() throws RecognitionException {
        EObject current = null;

        EObject iv_ruleNot = null;


        try {
            // InternalFSMDSL.g:3042:44: (iv_ruleNot= ruleNot EOF )
            // InternalFSMDSL.g:3043:2: iv_ruleNot= ruleNot EOF
            {
             newCompositeNode(grammarAccess.getNotRule()); 
            pushFollow(FOLLOW_1);
            iv_ruleNot=ruleNot();

            state._fsp--;

             current =iv_ruleNot; 
            match(input,EOF,FOLLOW_2); 

            }

        }

            catch (RecognitionException re) {
                recover(input,re);
                appendSkippedTokens();
            }
        finally {
        }
        return current;
    }
    // $ANTLR end "entryRuleNot"


    // $ANTLR start "ruleNot"
    // InternalFSMDSL.g:3049:1: ruleNot returns [EObject current=null] : ( () otherlv_1= '/' ( (lv_args_2_0= rulePrimary ) ) ) ;
    public final EObject ruleNot() throws RecognitionException {
        EObject current = null;

        Token otherlv_1=null;
        EObject lv_args_2_0 = null;



        	enterRule();

        try {
            // InternalFSMDSL.g:3055:2: ( ( () otherlv_1= '/' ( (lv_args_2_0= rulePrimary ) ) ) )
            // InternalFSMDSL.g:3056:2: ( () otherlv_1= '/' ( (lv_args_2_0= rulePrimary ) ) )
            {
            // InternalFSMDSL.g:3056:2: ( () otherlv_1= '/' ( (lv_args_2_0= rulePrimary ) ) )
            // InternalFSMDSL.g:3057:3: () otherlv_1= '/' ( (lv_args_2_0= rulePrimary ) )
            {
            // InternalFSMDSL.g:3057:3: ()
            // InternalFSMDSL.g:3058:4: 
            {

            				current = forceCreateModelElement(
            					grammarAccess.getNotAccess().getNotExprAction_0(),
            					current);
            			

            }

            otherlv_1=(Token)match(input,47,FOLLOW_12); 

            			newLeafNode(otherlv_1, grammarAccess.getNotAccess().getSolidusKeyword_1());
            		
            // InternalFSMDSL.g:3068:3: ( (lv_args_2_0= rulePrimary ) )
            // InternalFSMDSL.g:3069:4: (lv_args_2_0= rulePrimary )
            {
            // InternalFSMDSL.g:3069:4: (lv_args_2_0= rulePrimary )
            // InternalFSMDSL.g:3070:5: lv_args_2_0= rulePrimary
            {

            					newCompositeNode(grammarAccess.getNotAccess().getArgsPrimaryParserRuleCall_2_0());
            				
            pushFollow(FOLLOW_2);
            lv_args_2_0=rulePrimary();

            state._fsp--;


            					if (current==null) {
            						current = createModelElementForParent(grammarAccess.getNotRule());
            					}
            					add(
            						current,
            						"args",
            						lv_args_2_0,
            						"com.cburch.logisim.statemachine.FSMDSL.Primary");
            					afterParserOrEnumRuleCall();
            				

            }


            }


            }


            }


            	leaveRule();

        }

            catch (RecognitionException re) {
                recover(input,re);
                appendSkippedTokens();
            }
        finally {
        }
        return current;
    }
    // $ANTLR end "ruleNot"


    // $ANTLR start "entryRuleConstant"
    // InternalFSMDSL.g:3091:1: entryRuleConstant returns [EObject current=null] : iv_ruleConstant= ruleConstant EOF ;
    public final EObject entryRuleConstant() throws RecognitionException {
        EObject current = null;

        EObject iv_ruleConstant = null;


        try {
            // InternalFSMDSL.g:3091:49: (iv_ruleConstant= ruleConstant EOF )
            // InternalFSMDSL.g:3092:2: iv_ruleConstant= ruleConstant EOF
            {
             newCompositeNode(grammarAccess.getConstantRule()); 
            pushFollow(FOLLOW_1);
            iv_ruleConstant=ruleConstant();

            state._fsp--;

             current =iv_ruleConstant; 
            match(input,EOF,FOLLOW_2); 

            }

        }

            catch (RecognitionException re) {
                recover(input,re);
                appendSkippedTokens();
            }
        finally {
        }
        return current;
    }
    // $ANTLR end "entryRuleConstant"


    // $ANTLR start "ruleConstant"
    // InternalFSMDSL.g:3098:1: ruleConstant returns [EObject current=null] : ( () ( ( (lv_value_1_1= RULE_BIN | lv_value_1_2= RULE_HEX ) ) ) ) ;
    public final EObject ruleConstant() throws RecognitionException {
        EObject current = null;

        Token lv_value_1_1=null;
        Token lv_value_1_2=null;


        	enterRule();

        try {
            // InternalFSMDSL.g:3104:2: ( ( () ( ( (lv_value_1_1= RULE_BIN | lv_value_1_2= RULE_HEX ) ) ) ) )
            // InternalFSMDSL.g:3105:2: ( () ( ( (lv_value_1_1= RULE_BIN | lv_value_1_2= RULE_HEX ) ) ) )
            {
            // InternalFSMDSL.g:3105:2: ( () ( ( (lv_value_1_1= RULE_BIN | lv_value_1_2= RULE_HEX ) ) ) )
            // InternalFSMDSL.g:3106:3: () ( ( (lv_value_1_1= RULE_BIN | lv_value_1_2= RULE_HEX ) ) )
            {
            // InternalFSMDSL.g:3106:3: ()
            // InternalFSMDSL.g:3107:4: 
            {

            				current = forceCreateModelElement(
            					grammarAccess.getConstantAccess().getConstantAction_0(),
            					current);
            			

            }

            // InternalFSMDSL.g:3113:3: ( ( (lv_value_1_1= RULE_BIN | lv_value_1_2= RULE_HEX ) ) )
            // InternalFSMDSL.g:3114:4: ( (lv_value_1_1= RULE_BIN | lv_value_1_2= RULE_HEX ) )
            {
            // InternalFSMDSL.g:3114:4: ( (lv_value_1_1= RULE_BIN | lv_value_1_2= RULE_HEX ) )
            // InternalFSMDSL.g:3115:5: (lv_value_1_1= RULE_BIN | lv_value_1_2= RULE_HEX )
            {
            // InternalFSMDSL.g:3115:5: (lv_value_1_1= RULE_BIN | lv_value_1_2= RULE_HEX )
            int alt59=2;
            int LA59_0 = input.LA(1);

            if ( (LA59_0==RULE_BIN) ) {
                alt59=1;
            }
            else if ( (LA59_0==RULE_HEX) ) {
                alt59=2;
            }
            else {
                NoViableAltException nvae =
                    new NoViableAltException("", 59, 0, input);

                throw nvae;
            }
            switch (alt59) {
                case 1 :
                    // InternalFSMDSL.g:3116:6: lv_value_1_1= RULE_BIN
                    {
                    lv_value_1_1=(Token)match(input,RULE_BIN,FOLLOW_2); 

                    						newLeafNode(lv_value_1_1, grammarAccess.getConstantAccess().getValueBINTerminalRuleCall_1_0_0());
                    					

                    						if (current==null) {
                    							current = createModelElement(grammarAccess.getConstantRule());
                    						}
                    						setWithLastConsumed(
                    							current,
                    							"value",
                    							lv_value_1_1,
                    							"com.cburch.logisim.statemachine.FSMDSL.BIN");
                    					

                    }
                    break;
                case 2 :
                    // InternalFSMDSL.g:3131:6: lv_value_1_2= RULE_HEX
                    {
                    lv_value_1_2=(Token)match(input,RULE_HEX,FOLLOW_2); 

                    						newLeafNode(lv_value_1_2, grammarAccess.getConstantAccess().getValueHEXTerminalRuleCall_1_0_1());
                    					

                    						if (current==null) {
                    							current = createModelElement(grammarAccess.getConstantRule());
                    						}
                    						setWithLastConsumed(
                    							current,
                    							"value",
                    							lv_value_1_2,
                    							"com.cburch.logisim.statemachine.FSMDSL.HEX");
                    					

                    }
                    break;

            }


            }


            }


            }


            }


            	leaveRule();

        }

            catch (RecognitionException re) {
                recover(input,re);
                appendSkippedTokens();
            }
        finally {
        }
        return current;
    }
    // $ANTLR end "ruleConstant"


    // $ANTLR start "entryRuleConstantDef"
    // InternalFSMDSL.g:3152:1: entryRuleConstantDef returns [EObject current=null] : iv_ruleConstantDef= ruleConstantDef EOF ;
    public final EObject entryRuleConstantDef() throws RecognitionException {
        EObject current = null;

        EObject iv_ruleConstantDef = null;


        try {
            // InternalFSMDSL.g:3152:52: (iv_ruleConstantDef= ruleConstantDef EOF )
            // InternalFSMDSL.g:3153:2: iv_ruleConstantDef= ruleConstantDef EOF
            {
             newCompositeNode(grammarAccess.getConstantDefRule()); 
            pushFollow(FOLLOW_1);
            iv_ruleConstantDef=ruleConstantDef();

            state._fsp--;

             current =iv_ruleConstantDef; 
            match(input,EOF,FOLLOW_2); 

            }

        }

            catch (RecognitionException re) {
                recover(input,re);
                appendSkippedTokens();
            }
        finally {
        }
        return current;
    }
    // $ANTLR end "entryRuleConstantDef"


    // $ANTLR start "ruleConstantDef"
    // InternalFSMDSL.g:3159:1: ruleConstantDef returns [EObject current=null] : (otherlv_0= 'define' ( (lv_name_1_0= RULE_ID ) ) otherlv_2= '=' ( (lv_value_3_0= ruleConstant ) ) ) ;
    public final EObject ruleConstantDef() throws RecognitionException {
        EObject current = null;

        Token otherlv_0=null;
        Token lv_name_1_0=null;
        Token otherlv_2=null;
        EObject lv_value_3_0 = null;



        	enterRule();

        try {
            // InternalFSMDSL.g:3165:2: ( (otherlv_0= 'define' ( (lv_name_1_0= RULE_ID ) ) otherlv_2= '=' ( (lv_value_3_0= ruleConstant ) ) ) )
            // InternalFSMDSL.g:3166:2: (otherlv_0= 'define' ( (lv_name_1_0= RULE_ID ) ) otherlv_2= '=' ( (lv_value_3_0= ruleConstant ) ) )
            {
            // InternalFSMDSL.g:3166:2: (otherlv_0= 'define' ( (lv_name_1_0= RULE_ID ) ) otherlv_2= '=' ( (lv_value_3_0= ruleConstant ) ) )
            // InternalFSMDSL.g:3167:3: otherlv_0= 'define' ( (lv_name_1_0= RULE_ID ) ) otherlv_2= '=' ( (lv_value_3_0= ruleConstant ) )
            {
            otherlv_0=(Token)match(input,48,FOLLOW_8); 

            			newLeafNode(otherlv_0, grammarAccess.getConstantDefAccess().getDefineKeyword_0());
            		
            // InternalFSMDSL.g:3171:3: ( (lv_name_1_0= RULE_ID ) )
            // InternalFSMDSL.g:3172:4: (lv_name_1_0= RULE_ID )
            {
            // InternalFSMDSL.g:3172:4: (lv_name_1_0= RULE_ID )
            // InternalFSMDSL.g:3173:5: lv_name_1_0= RULE_ID
            {
            lv_name_1_0=(Token)match(input,RULE_ID,FOLLOW_13); 

            					newLeafNode(lv_name_1_0, grammarAccess.getConstantDefAccess().getNameIDTerminalRuleCall_1_0());
            				

            					if (current==null) {
            						current = createModelElement(grammarAccess.getConstantDefRule());
            					}
            					setWithLastConsumed(
            						current,
            						"name",
            						lv_name_1_0,
            						"org.eclipse.xtext.common.Terminals.ID");
            				

            }


            }

            otherlv_2=(Token)match(input,18,FOLLOW_51); 

            			newLeafNode(otherlv_2, grammarAccess.getConstantDefAccess().getEqualsSignKeyword_2());
            		
            // InternalFSMDSL.g:3193:3: ( (lv_value_3_0= ruleConstant ) )
            // InternalFSMDSL.g:3194:4: (lv_value_3_0= ruleConstant )
            {
            // InternalFSMDSL.g:3194:4: (lv_value_3_0= ruleConstant )
            // InternalFSMDSL.g:3195:5: lv_value_3_0= ruleConstant
            {

            					newCompositeNode(grammarAccess.getConstantDefAccess().getValueConstantParserRuleCall_3_0());
            				
            pushFollow(FOLLOW_2);
            lv_value_3_0=ruleConstant();

            state._fsp--;


            					if (current==null) {
            						current = createModelElementForParent(grammarAccess.getConstantDefRule());
            					}
            					set(
            						current,
            						"value",
            						lv_value_3_0,
            						"com.cburch.logisim.statemachine.FSMDSL.Constant");
            					afterParserOrEnumRuleCall();
            				

            }


            }


            }


            }


            	leaveRule();

        }

            catch (RecognitionException re) {
                recover(input,re);
                appendSkippedTokens();
            }
        finally {
        }
        return current;
    }
    // $ANTLR end "ruleConstantDef"

    // Delegated rules


    protected DFA1 dfa1 = new DFA1(this);
    static final String dfa_1s = "\36\uffff";
    static final String dfa_2s = "\1\2\35\uffff";
    static final String dfa_3s = "\1\15\2\uffff\1\17\1\uffff\1\4\1\15\1\22\1\4\1\6\1\15\1\4\2\16\1\5\1\4\2\uffff\1\60\1\17\1\15\1\4\1\16\1\5\1\22\1\17\1\6\3\16";
    static final String dfa_4s = "\1\60\2\uffff\1\60\1\uffff\1\4\1\15\1\22\1\17\1\7\1\17\1\57\2\17\1\5\1\4\2\uffff\1\60\2\17\1\4\1\17\1\5\1\22\1\17\1\7\3\17";
    static final String dfa_5s = "\1\uffff\1\1\1\2\1\uffff\1\5\13\uffff\1\4\1\3\14\uffff";
    static final String dfa_6s = "\36\uffff}>";
    static final String[] dfa_7s = {
            "\1\3\3\uffff\1\4\7\uffff\2\1\25\uffff\1\2",
            "",
            "",
            "\1\6\40\uffff\1\5",
            "",
            "\1\7",
            "\1\10",
            "\1\11",
            "\1\12\12\uffff\1\13",
            "\1\14\1\15",
            "\1\16\1\17\1\13",
            "\1\20\1\uffff\2\20\5\uffff\1\21\10\uffff\1\20\4\uffff\1\20\16\uffff\2\20\3\uffff\1\20",
            "\1\22\1\6",
            "\1\22\1\6",
            "\1\23",
            "\1\24",
            "",
            "",
            "\1\25",
            "\1\26",
            "\1\27\1\17\1\13",
            "\1\30",
            "\1\17\1\13",
            "\1\31",
            "\1\32",
            "\1\33",
            "\1\34\1\35",
            "\1\17\1\13",
            "\1\22\1\6",
            "\1\22\1\6"
    };

    static final short[] dfa_1 = DFA.unpackEncodedString(dfa_1s);
    static final short[] dfa_2 = DFA.unpackEncodedString(dfa_2s);
    static final char[] dfa_3 = DFA.unpackEncodedStringToUnsignedChars(dfa_3s);
    static final char[] dfa_4 = DFA.unpackEncodedStringToUnsignedChars(dfa_4s);
    static final short[] dfa_5 = DFA.unpackEncodedString(dfa_5s);
    static final short[] dfa_6 = DFA.unpackEncodedString(dfa_6s);
    static final short[][] dfa_7 = unpackEncodedStringArray(dfa_7s);

    class DFA1 extends DFA {

        public DFA1(BaseRecognizer recognizer) {
            this.recognizer = recognizer;
            this.decisionNumber = 1;
            this.eot = dfa_1;
            this.eof = dfa_2;
            this.min = dfa_3;
            this.max = dfa_4;
            this.accept = dfa_5;
            this.special = dfa_6;
            this.transition = dfa_7;
        }
        public String getDescription() {
            return "78:2: (this_FSM_0= ruleFSM | this_ConstantDefList_1= ruleConstantDefList | this_CommandStmt_2= ruleCommandStmt | this_PredicateStmt_3= rulePredicateStmt | this_EQNSpec_4= ruleEQNSpec )";
        }
    }
 

    public static final BitSet FOLLOW_1 = new BitSet(new long[]{0x0000000000000000L});
    public static final BitSet FOLLOW_2 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_3 = new BitSet(new long[]{0x0001000000008000L});
    public static final BitSet FOLLOW_4 = new BitSet(new long[]{0x000000000000C000L});
    public static final BitSet FOLLOW_5 = new BitSet(new long[]{0x0001000000000000L});
    public static final BitSet FOLLOW_6 = new BitSet(new long[]{0x0000000000002000L});
    public static final BitSet FOLLOW_7 = new BitSet(new long[]{0x0000000000008010L});
    public static final BitSet FOLLOW_8 = new BitSet(new long[]{0x0000000000000010L});
    public static final BitSet FOLLOW_9 = new BitSet(new long[]{0x0000000000010012L});
    public static final BitSet FOLLOW_10 = new BitSet(new long[]{0x0000000000000012L});
    public static final BitSet FOLLOW_11 = new BitSet(new long[]{0x0000000000010000L});
    public static final BitSet FOLLOW_12 = new BitSet(new long[]{0x00008C00084000D0L});
    public static final BitSet FOLLOW_13 = new BitSet(new long[]{0x0000000000040000L});
    public static final BitSet FOLLOW_14 = new BitSet(new long[]{0x0000000000010010L});
    public static final BitSet FOLLOW_15 = new BitSet(new long[]{0x0000000000014000L});
    public static final BitSet FOLLOW_16 = new BitSet(new long[]{0x0000000000080000L});
    public static final BitSet FOLLOW_17 = new BitSet(new long[]{0x0000000001400010L});
    public static final BitSet FOLLOW_18 = new BitSet(new long[]{0x0000000000100002L});
    public static final BitSet FOLLOW_19 = new BitSet(new long[]{0x0000000000200002L});
    public static final BitSet FOLLOW_20 = new BitSet(new long[]{0x0000000000800000L});
    public static final BitSet FOLLOW_21 = new BitSet(new long[]{0x0000000408000000L});
    public static final BitSet FOLLOW_22 = new BitSet(new long[]{0x0000000008000000L});
    public static final BitSet FOLLOW_23 = new BitSet(new long[]{0x0001000030000000L});
    public static final BitSet FOLLOW_24 = new BitSet(new long[]{0x0000000070000010L});
    public static final BitSet FOLLOW_25 = new BitSet(new long[]{0x0000000000000020L});
    public static final BitSet FOLLOW_26 = new BitSet(new long[]{0x0000000080000000L});
    public static final BitSet FOLLOW_27 = new BitSet(new long[]{0x0000000900000000L});
    public static final BitSet FOLLOW_28 = new BitSet(new long[]{0x0000000000002002L});
    public static final BitSet FOLLOW_29 = new BitSet(new long[]{0x0000000000008000L});
    public static final BitSet FOLLOW_30 = new BitSet(new long[]{0x0000000400012000L});
    public static final BitSet FOLLOW_31 = new BitSet(new long[]{0x0000000400010000L});
    public static final BitSet FOLLOW_32 = new BitSet(new long[]{0x0000000100000010L});
    public static final BitSet FOLLOW_33 = new BitSet(new long[]{0x0000000000004000L});
    public static final BitSet FOLLOW_34 = new BitSet(new long[]{0x0000000408040000L});
    public static final BitSet FOLLOW_35 = new BitSet(new long[]{0x0000000000000040L});
    public static final BitSet FOLLOW_36 = new BitSet(new long[]{0x0000001300000000L});
    public static final BitSet FOLLOW_37 = new BitSet(new long[]{0x0000001100000000L});
    public static final BitSet FOLLOW_38 = new BitSet(new long[]{0x0000020100000010L});
    public static final BitSet FOLLOW_39 = new BitSet(new long[]{0x0000000100000000L});
    public static final BitSet FOLLOW_40 = new BitSet(new long[]{0x0000002000002000L});
    public static final BitSet FOLLOW_41 = new BitSet(new long[]{0x0000002000000000L});
    public static final BitSet FOLLOW_42 = new BitSet(new long[]{0x000000C400000000L});
    public static final BitSet FOLLOW_43 = new BitSet(new long[]{0x000000C000000000L});
    public static final BitSet FOLLOW_44 = new BitSet(new long[]{0x0000008000000002L});
    public static final BitSet FOLLOW_45 = new BitSet(new long[]{0x0000000400000010L});
    public static final BitSet FOLLOW_46 = new BitSet(new long[]{0x0000010400010000L});
    public static final BitSet FOLLOW_47 = new BitSet(new long[]{0x0000020000000000L});
    public static final BitSet FOLLOW_48 = new BitSet(new long[]{0x0000000100004000L});
    public static final BitSet FOLLOW_49 = new BitSet(new long[]{0x0000100000000002L});
    public static final BitSet FOLLOW_50 = new BitSet(new long[]{0x0000600000000002L});
    public static final BitSet FOLLOW_51 = new BitSet(new long[]{0x00000000000000C0L});

}