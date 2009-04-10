

package org.jquantlib.math;


public interface E_IUnaryFunction<ParameterType, ReturnType> {


    
	public ReturnType evaluate(ParameterType x);
    void setBinaryFunction(E_IBinaryFunction<ParameterType, ReturnType> binaryFunction);
    void setBoundedValue(ParameterType x);
	
	
	//boolean isFailed() // TODO is error handling needed?
}