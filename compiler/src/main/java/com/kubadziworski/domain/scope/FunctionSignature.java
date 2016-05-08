package com.kubadziworski.domain.scope;

import com.kubadziworski.domain.expression.FunctionParameter;
import com.kubadziworski.domain.type.Type;
import com.kubadziworski.exception.ParameterForNameNotFoundException;

import java.util.Collections;
import java.util.List;

import static java.util.stream.Collectors.toList;

/**
 * Created by kuba on 06.04.16.
 */
public class FunctionSignature {
    private final String name;
    private final List<FunctionParameter> parameters;
    private final Type returnType;

    public FunctionSignature(String name, List<FunctionParameter> parameters, Type returnType) {
        this.name = name;
        this.parameters = parameters;
        this.returnType = returnType;
    }

    public String getName() {
        return name;
    }

    public List<FunctionParameter> getParameters() {
        return Collections.unmodifiableList(parameters);
    }

    public FunctionParameter getParameterForName(String name) {
        return parameters.stream()
                .filter(param -> param.getName().equals(name))
                .findFirst()
                .orElseThrow(() -> new ParameterForNameNotFoundException(name,parameters));
    }

    public int getIndexOfParameter(String parameterName) {
        FunctionParameter parameter = getParameterForName(parameterName);
        return parameters.indexOf(parameter);
    }

    public boolean matches(String otherSignatureName, List<Type> otherSignatureParams) {
        boolean namesAreEqual = this.name.equals(otherSignatureName);
        if(!namesAreEqual) return false;
        List<Type> paramTypes = parameters.stream().map(p -> p.getType()).collect(toList());
        boolean sameParameterTypes = paramTypes.containsAll(otherSignatureParams) && otherSignatureParams.containsAll(paramTypes);
        return sameParameterTypes;
    }

    public Type getReturnType() {
        return returnType;
    }
}
