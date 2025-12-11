package vat.codegen.utils;

import com.palantir.javapoet.TypeName;

///
/// @author Zen.Liu
/// @since 2025-12-03
public record Validator(TypeName type, String field, boolean construct) {

}
