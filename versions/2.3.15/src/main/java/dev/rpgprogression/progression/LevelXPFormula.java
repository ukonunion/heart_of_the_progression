package dev.rpgprogression.progression;

import java.util.*;
import java.util.function.*;

/** Small arithmetic grammar: no scripting, reflection, file access or unbounded evaluation. */
public final class LevelXPFormula {
    public static final String DEFAULT="base * level ^ 2";
    private record Values(double level,double base,double multiplier){}
    private record Node(ToDoubleFunction<Values> value,boolean levelDependent){double evaluate(Values values){return value.applyAsDouble(values);}}
    private record Cached(String source,Node expression){}
    private static volatile Cached cached;
    public static boolean valid(String source){try{new Parser(source).parse();return true;}catch(IllegalArgumentException error){return false;}}
    private static Node expression(String source){
        var entry=cached;if(entry!=null&&Objects.equals(entry.source,source))return entry.expression;
        Node node;try{node=new Parser(source).parse();}catch(IllegalArgumentException error){node=new Node(v->Double.NaN,true);}
        cached=new Cached(source,node);return node;
    }
    public static double evaluate(String source,int level,double base,double multiplier){return expression(source).evaluate(new Values(Math.max(1,level),base,multiplier));}
    public static boolean levelIndependent(String source){return !expression(source).levelDependent;}
    private static final class Parser {
        private final String text;private int at,nodes,depth;
        private Parser(String text){if(text==null||text.isBlank()||text.length()>1024)throw new IllegalArgumentException("Formula must contain 1..1024 characters");this.text=text;}
        private IllegalArgumentException error(){return new IllegalArgumentException("Invalid XP formula at character "+at);}
        private void space(){while(at<text.length()&&Character.isWhitespace(text.charAt(at)))at++;}
        private boolean take(char c){space();if(at<text.length()&&text.charAt(at)==c){at++;return true;}return false;}
        private Node node(ToDoubleFunction<Values> value,boolean dependent){if(++nodes>256)throw error();return new Node(value,dependent);}
        private Node constant(double value){return node(v->value,false);}
        private Node unary(Node child,DoubleUnaryOperator op){return node(v->op.applyAsDouble(child.evaluate(v)),child.levelDependent);}
        private Node binary(Node a,Node b,DoubleBinaryOperator op){return node(v->op.applyAsDouble(a.evaluate(v),b.evaluate(v)),a.levelDependent||b.levelDependent);}
        private Node parse(){Node result=sum();space();if(at!=text.length())throw error();return result;}
        private Node sum(){Node result=product();while(true){if(take('+'))result=binary(result,product(),(a,b)->a+b);else if(take('-'))result=binary(result,product(),(a,b)->a-b);else return result;}}
        private Node product(){Node result=prefix();while(true){if(take('*'))result=binary(result,prefix(),(a,b)->a*b);else if(take('/'))result=binary(result,prefix(),(a,b)->a/b);else if(take('%'))result=binary(result,prefix(),(a,b)->a%b);else return result;}}
        private Node prefix(){
            if(++depth>32)throw error();
            try{if(take('+'))return prefix();if(take('-'))return unary(prefix(),a->-a);Node result=primary();return take('^')?binary(result,prefix(),Math::pow):result;}
            finally{depth--;}
        }
        private Node primary(){
            if(take('(')){var result=sum();if(!take(')'))throw error();return result;}
            space();if(at>=text.length())throw error();char c=text.charAt(at);
            if(Character.isDigit(c)||c=='.'){
                int start=at;while(at<text.length()&&(Character.isDigit(text.charAt(at))||text.charAt(at)=='.'))at++;
                if(at<text.length()&&(text.charAt(at)=='e'||text.charAt(at)=='E')){at++;if(at<text.length()&&(text.charAt(at)=='+'||text.charAt(at)=='-'))at++;while(at<text.length()&&Character.isDigit(text.charAt(at)))at++;}
                double value;try{value=Double.parseDouble(text.substring(start,at));}catch(NumberFormatException malformed){throw error();}
                if(!Double.isFinite(value))throw error();return constant(value);
            }
            if(!Character.isLetter(c))throw error();int start=at;while(at<text.length()&&(Character.isLetterOrDigit(text.charAt(at))||text.charAt(at)=='_'))at++;
            String name=text.substring(start,at);
            if(!take('('))return switch(name){case "level"->node(Values::level,true);case "base"->node(Values::base,false);case "multiplier"->node(Values::multiplier,false);case "pi"->constant(Math.PI);case "e"->constant(Math.E);default->throw error();};
            var args=new ArrayList<Node>();if(!take(')')){do{args.add(sum());if(args.size()>3)throw error();}while(take(','));if(!take(')'))throw error();}
            int count=args.size();
            if(count==1){var a=args.get(0);return switch(name){case "abs"->unary(a,Math::abs);case "sqrt"->unary(a,Math::sqrt);case "floor"->unary(a,Math::floor);case "ceil"->unary(a,Math::ceil);case "round"->unary(a,x->Math.floor(x+.5));case "log"->unary(a,Math::log);case "exp"->unary(a,Math::exp);default->throw error();};}
            if(count==2){var a=args.get(0);var b=args.get(1);return switch(name){case "min"->binary(a,b,Math::min);case "max"->binary(a,b,Math::max);case "pow"->binary(a,b,Math::pow);default->throw error();};}
            if(count==3&&name.equals("clamp")){var a=args.get(0);var low=args.get(1);var high=args.get(2);return node(v->Math.max(low.evaluate(v),Math.min(high.evaluate(v),a.evaluate(v))),a.levelDependent||low.levelDependent||high.levelDependent);}
            throw error();
        }
    }
    private LevelXPFormula(){}
}
