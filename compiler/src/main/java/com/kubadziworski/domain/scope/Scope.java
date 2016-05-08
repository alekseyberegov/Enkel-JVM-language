package com.kubadziworski.domain.scope;

import com.google.common.collect.Lists;
import com.kubadziworski.domain.expression.Expression;
import com.kubadziworski.domain.global.MetaData;
import com.kubadziworski.domain.type.BultInType;
import com.kubadziworski.domain.type.ClassType;
import com.kubadziworski.domain.type.Type;
import com.kubadziworski.exception.ClassNotFoundForNameException;
import com.kubadziworski.exception.LocalVariableNotFoundException;
import com.kubadziworski.exception.MethodSignatureNotFoundException;
import com.kubadziworski.util.ReflectionObjectToSignatureMapper;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.ConstructorUtils;
import org.apache.commons.lang3.reflect.MethodUtils;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.*;

import static java.util.stream.Collectors.toList;

/**
 * Created by kuba on 02.04.16.
 */
public class Scope {
    private List<LocalVariable> localVariables;
    private List<FunctionSignature> functionSignatures;
    private final MetaData metaData;

    public Scope(MetaData metaData) {
        localVariables = new ArrayList<>();
        functionSignatures = new ArrayList<>();
        this.metaData = metaData;
    }

    public Scope(Scope scope) {
        metaData = scope.metaData;
        localVariables =  Lists.newArrayList(scope.localVariables);
        functionSignatures = Lists.newArrayList(scope.functionSignatures);
    }

    public void addSignature(FunctionSignature signature) {
        functionSignatures.add(signature);
    }

    public boolean parameterLessSignatureExists(String identifier) {
        return signatureExists(identifier,Collections.emptyList());
    }

    public boolean signatureExists(String identifier, List<Type> parameters) {
        if(identifier.equals("super")) return true;
        return functionSignatures.stream()
                .anyMatch(signature -> signature.matches(identifier,parameters));
    }

    public FunctionSignature getMethodCallSignatureWithoutParameters(String identifier) {
        return getMethodCallSignature(identifier, Collections.<Type>emptyList());
    }

    public FunctionSignature getMethodCallSignature(String identifier, Collection<Expression> arguments){
        List<Type> argumentTypes = arguments.stream().map(e -> e.getType()).collect(toList());
        return getMethodCallSignature(identifier, argumentTypes);
    }

    public FunctionSignature getMethodCallSignature(String identifier,List<Type> parameterTypes) {
        if(identifier.equals("super")){
            return new FunctionSignature("super", Collections.emptyList(), BultInType.VOID);
        }
        return functionSignatures.stream()
                .filter(signature -> signature.matches(identifier,parameterTypes))
                .findFirst()
                .orElseThrow(() -> new MethodSignatureNotFoundException(this, identifier,parameterTypes));
    }

    private String getSuperClassName() {
        return metaData.getSuperClassName();
    }

    public void addLocalVariable(LocalVariable localVariable) {
        localVariables.add(localVariable);
    }

    public LocalVariable getLocalVariable(String varName) {
        return localVariables.stream()
                .filter(variable -> variable.getName().equals(varName))
                .findFirst()
                .orElseThrow(() -> new LocalVariableNotFoundException(this, varName));
    }

    public boolean localVariableExists(String varName) {
        return localVariables.stream()
                .anyMatch(variable -> variable.getName().equals(varName));
    }

    public int getLocalVariableIndex(String varName) {
        LocalVariable localVariable = getLocalVariable(varName);
        return localVariables.indexOf(localVariable);
    }

    Optional<FunctionSignature> getSignatureOnClassPath(String fullMethodName) {
        String methodName = StringUtils.removePattern(fullMethodName,".*\\.");
        String className = fullMethodName; // StringUtils.difference(fullMethodName, methodName);
        Class<?> methodOwnerClass = null;
        try {
            methodOwnerClass = ClassUtils.getClass(className);
        } catch (ClassNotFoundException e) {
            throw new ClassNotFoundForNameException(className);
        }
        Method accessibleMethod = MethodUtils.getAccessibleMethod(methodOwnerClass, methodName);
        if(accessibleMethod != null) {
            FunctionSignature signature = ReflectionObjectToSignatureMapper.fromMethod(accessibleMethod);
            return Optional.of(signature);
        }
        Constructor<?> accessibleConstructor = ConstructorUtils.getAccessibleConstructor(methodOwnerClass);
        if(accessibleConstructor != null) {
            FunctionSignature signature = ReflectionObjectToSignatureMapper.fromConstructor(accessibleConstructor);
            return Optional.of(signature);
        }
        return Optional.empty();
    }

    public String getClassName() {
        return metaData.getClassName();
    }

    public String getSuperClassInternalName() {
        return new ClassType(getSuperClassName()).getInternalName();
    }

    public Type getClassType() {
        String className = getClassName();
        return new ClassType(className);
    }

    public String getClassInternalName() {
        return getClassType().getInternalName();
    }
}