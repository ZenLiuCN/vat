package vat.api.store;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import org.jooq.lambda.tuple.*;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

///
/// @author Zen.Liu
/// @since 2025-10-23


public sealed interface Stages {
    Status status();

    non-sealed interface Store<T> extends vat.api.Store<T>, Stages, Joiner<T>, Inserted<T> {
        /// actor should the ID of actor or null
        default Store<T> withActor(@Nullable Object actor) {
            status().withActor(actor);
            return this;
        }
        /// fetch values with history field. default only fetch an empty object.
        /// *note*: this only effect for full entity access.
        default Store<T> withHistory(){
            status().withHistory();
            return this;
        }
    }

    non-sealed interface Modified<T> extends Stages {
        default Future<Integer> justSet(Statement.SetStmt[] sets) {
            return status().justSet(sets);
        }

    }

    non-sealed interface Inserted<T> extends Stages {
        default Future<Boolean> justPut(StmtAssign[] assigns) {
            return status().justPut(assigns);
        }

        default <ID> Future<ID> put(StmtAssign[] assigns) {
            return status().put(assigns);
        }


        default Future<Integer> justPutMany(StmtAssign[][] assigns) {
            return status().justPutMany(assigns);
        }

        default <ID> Future<List<ID>> putMany(StmtAssign[][] assigns) {
            return status().putMany(assigns);
        }
    }

    non-sealed interface Removed<T> extends Stages {
        default Future<Integer> remove(boolean permanent) {
            return status().remove(permanent);
        }
    }

    non-sealed interface Joiner<T> extends Stages, Filter<T> {
        /// T inner join to R with condition
        @SuppressWarnings("unchecked")
        default <R> Joined.Joined2<T, R> join(Model<R> model, Value.BooleanValue condition) {
            return (Joined.Joined2<T, R>) status().join(model, condition);
        }

        /// T left outer join with R
        @SuppressWarnings("unchecked")
        default <R> Joined.Joined2<T, R> joinWith(Model<R> model, Value.BooleanValue condition) {
            return (Joined.Joined2<T, R>) status().join(model, condition);
        }

        /// T right outer join with R
        @SuppressWarnings("unchecked")
        default <R> Joined.Joined2<T, R> joinTo(Model<R> model, Value.BooleanValue condition) {
            return (Joined.Joined2<T, R>) status().join(model, condition);
        }
    }

    non-sealed interface Joined<T> extends Stages, Filter<T> {
        //region tuples
        non-sealed interface Joined2<T, R0> extends Stages, Joined<Tuple2<T, R0>> {
            /// @see Joiner#join(Model, Value.BooleanValue)
            @SuppressWarnings("unchecked")
            default <R> Joined3<T, R0, R> join(Model<R> model, Value.BooleanValue condition) {
                return (Joined3<T, R0, R>) status().join(model, condition);
            }

            /// @see Joiner#joinWith(Model, Value.BooleanValue)
            @SuppressWarnings("unchecked")
            default <R> Joined3<T, R0, R> joinWith(Model<R> model, Value.BooleanValue condition) {
                return (Joined3<T, R0, R>) status().joinWith(model, condition);
            }

            /// @see Joiner#joinTo(Model, Value.BooleanValue)
            @SuppressWarnings("unchecked")
            default <R> Joined3<T, R0, R> joinTo(Model<R> model, Value.BooleanValue condition) {
                return (Joined3<T, R0, R>) status().joinTo(model, condition);
            }
        }

        non-sealed interface Joined3<T, R0, R1> extends Stages, Joined<Tuple3<T, R0, R1>> {
            /// @see Joiner#join(Model, Value.BooleanValue)
            @SuppressWarnings("unchecked")
            default <R> Joined4<T, R0, R1, R> join(Model<R> model, Value.BooleanValue condition) {
                return (Joined4<T, R0, R1, R>) status().join(model, condition);
            }

            /// @see Joiner#joinWith(Model, Value.BooleanValue)
            @SuppressWarnings("unchecked")
            default <R> Joined4<T, R0, R1, R> joinWith(Model<R> model, Value.BooleanValue condition) {
                return (Joined4<T, R0, R1, R>) status().joinWith(model, condition);
            }

            /// @see Joiner#joinTo(Model, Value.BooleanValue)
            @SuppressWarnings("unchecked")
            default <R> Joined4<T, R0, R1, R> joinTo(Model<R> model, Value.BooleanValue condition) {
                return (Joined4<T, R0, R1, R>) status().joinTo(model, condition);
            }
        }

        non-sealed interface Joined4<T, R0, R1, R2> extends Stages, Joined<Tuple4<T, R0, R1, R2>> {
            /// @see Joiner#join(Model, Value.BooleanValue)
            @SuppressWarnings("unchecked")
            default <R> Joined5<T, R0, R1, R2, R> join(Model<R> model, Value.BooleanValue condition) {
                return (Joined5<T, R0, R1, R2, R>) status().join(model, condition);
            }

            /// @see Joiner#joinWith(Model, Value.BooleanValue)
            @SuppressWarnings("unchecked")
            default <R> Joined5<T, R0, R1, R2, R> joinWith(Model<R> model, Value.BooleanValue condition) {
                return (Joined5<T, R0, R1, R2, R>) status().joinWith(model, condition);
            }

            /// @see Joiner#joinTo(Model, Value.BooleanValue)
            @SuppressWarnings("unchecked")
            default <R> Joined5<T, R0, R1, R2, R> joinTo(Model<R> model, Value.BooleanValue condition) {
                return (Joined5<T, R0, R1, R2, R>) status().joinTo(model, condition);
            }
        }

        non-sealed interface Joined5<T, R0, R1, R2, R3> extends Stages, Joined<Tuple5<T, R0, R1, R2, R3>> {
            /// @see Joiner#join(Model, Value.BooleanValue)
            @SuppressWarnings("unchecked")
            default <R> Joined6<T, R0, R1, R2, R3, R> join(Model<R> model, Value.BooleanValue condition) {
                return (Joined6<T, R0, R1, R2, R3, R>) status().join(model, condition);
            }

            /// @see Joiner#joinWith(Model, Value.BooleanValue)
            @SuppressWarnings("unchecked")
            default <R> Joined6<T, R0, R1, R2, R3, R> joinWith(Model<R> model, Value.BooleanValue condition) {
                return (Joined6<T, R0, R1, R2, R3, R>) status().joinWith(model, condition);
            }

            /// @see Joiner#joinTo(Model, Value.BooleanValue)
            @SuppressWarnings("unchecked")
            default <R> Joined6<T, R0, R1, R2, R3, R> joinTo(Model<R> model, Value.BooleanValue condition) {
                return (Joined6<T, R0, R1, R2, R3, R>) status().joinTo(model, condition);
            }
        }

        non-sealed interface Joined6<T, R0, R1, R2, R3, R4> extends Stages, Joined<Tuple6<T, R0, R1, R2, R3, R4>> {
            /// @see Joiner#join(Model, Value.BooleanValue)
            @SuppressWarnings("unchecked")
            default <R> Joined7<T, R0, R1, R2, R3, R4, R> join(Model<R> model, Value.BooleanValue condition) {
                return (Joined7<T, R0, R1, R2, R3, R4, R>) status().join(model, condition);
            }

            /// @see Joiner#joinWith(Model, Value.BooleanValue)
            @SuppressWarnings("unchecked")
            default <R> Joined7<T, R0, R1, R2, R3, R4, R> joinWith(Model<R> model, Value.BooleanValue condition) {
                return (Joined7<T, R0, R1, R2, R3, R4, R>) status().joinWith(model, condition);
            }

            /// @see Joiner#joinTo(Model, Value.BooleanValue)
            @SuppressWarnings("unchecked")
            default <R> Joined7<T, R0, R1, R2, R3, R4, R> joinTo(Model<R> model, Value.BooleanValue condition) {
                return (Joined7<T, R0, R1, R2, R3, R4, R>) status().joinTo(model, condition);
            }
        }

        non-sealed interface Joined7<T, R0, R1, R2, R3, R4, R5> extends Stages,
                Joined<Tuple7<T, R0, R1, R2, R3, R4, R5>> {
            /// @see Joiner#join(Model, Value.BooleanValue)
            @SuppressWarnings("unchecked")
            default <R> Joined8<T, R0, R1, R2, R3, R4, R5, R> join(Model<R> model, Value.BooleanValue condition) {
                return (Joined8<T, R0, R1, R2, R3, R4, R5, R>) status().join(model, condition);
            }

            /// @see Joiner#joinWith(Model, Value.BooleanValue)
            @SuppressWarnings("unchecked")
            default <R> Joined8<T, R0, R1, R2, R3, R4, R5, R> joinWith(Model<R> model, Value.BooleanValue condition) {
                return (Joined8<T, R0, R1, R2, R3, R4, R5, R>) status().joinWith(model, condition);
            }

            /// @see Joiner#joinTo(Model, Value.BooleanValue)
            @SuppressWarnings("unchecked")
            default <R> Joined8<T, R0, R1, R2, R3, R4, R5, R> joinTo(Model<R> model, Value.BooleanValue condition) {
                return (Joined8<T, R0, R1, R2, R3, R4, R5, R>) status().joinTo(model, condition);
            }
        }

        non-sealed interface Joined8<T, R0, R1, R2, R3, R4, R5, R6> extends Stages,
                Joined<Tuple8<T, R0, R1, R2, R3, R4, R5, R6>> {
            /// @see Joiner#join(Model, Value.BooleanValue)
            @SuppressWarnings("unchecked")
            default <R> Joined9<T, R0, R1, R2, R3, R4, R5, R6, R> join(Model<R> model, Value.BooleanValue condition) {
                return (Joined9<T, R0, R1, R2, R3, R4, R5, R6, R>) status().join(model, condition);
            }

            /// @see Joiner#joinWith(Model, Value.BooleanValue)
            @SuppressWarnings("unchecked")
            default <R> Joined9<T, R0, R1, R2, R3, R4, R5, R6, R> joinWith(Model<R> model,
                                                                           Value.BooleanValue condition) {
                return (Joined9<T, R0, R1, R2, R3, R4, R5, R6, R>) status().joinWith(model, condition);
            }

            /// @see Joiner#joinTo(Model, Value.BooleanValue)
            @SuppressWarnings("unchecked")
            default <R> Joined9<T, R0, R1, R2, R3, R4, R5, R6, R> joinTo(Model<R> model, Value.BooleanValue condition) {
                return (Joined9<T, R0, R1, R2, R3, R4, R5, R6, R>) status().joinTo(model, condition);
            }
        }

        non-sealed interface Joined9<T, R0, R1, R2, R3, R4, R5, R6, R7> extends Stages,
                Joined<Tuple9<T, R0, R1, R2, R3, R4, R5, R6, R7>> {
            /// @see Joiner#join(Model, Value.BooleanValue)
            @SuppressWarnings("unchecked")
            default <R> Joined10<T, R0, R1, R2, R3, R4, R5, R6, R7, R> join(Model<R> model,
                                                                            Value.BooleanValue condition) {
                return (Joined10<T, R0, R1, R2, R3, R4, R5, R6, R7, R>) status().join(model, condition);
            }

            /// @see Joiner#joinWith(Model, Value.BooleanValue)
            @SuppressWarnings("unchecked")
            default <R> Joined10<T, R0, R1, R2, R3, R4, R5, R6, R7, R> joinWith(Model<R> model,
                                                                                Value.BooleanValue condition) {
                return (Joined10<T, R0, R1, R2, R3, R4, R5, R6, R7, R>) status().joinWith(model, condition);
            }

            /// @see Joiner#joinTo(Model, Value.BooleanValue)
            @SuppressWarnings("unchecked")
            default <R> Joined10<T, R0, R1, R2, R3, R4, R5, R6, R7, R> joinTo(Model<R> model,
                                                                              Value.BooleanValue condition) {
                return (Joined10<T, R0, R1, R2, R3, R4, R5, R6, R7, R>) status().joinTo(model, condition);
            }
        }

        non-sealed interface Joined10<T, R0, R1, R2, R3, R4, R5, R6, R7, R8> extends Stages,
                Joined<Tuple10<T, R0, R1, R2, R3, R4, R5, R6, R7, R8>> {
            /// @see Joiner#join(Model, Value.BooleanValue)
            @SuppressWarnings("unchecked")
            default <R> Joined11<T, R0, R1, R2, R3, R4, R5, R6, R7, R8, R> join(Model<R> model,
                                                                                Value.BooleanValue condition) {
                return (Joined11<T, R0, R1, R2, R3, R4, R5, R6, R7, R8, R>) status().join(model, condition);
            }

            /// @see Joiner#joinWith(Model, Value.BooleanValue)
            @SuppressWarnings("unchecked")
            default <R> Joined11<T, R0, R1, R2, R3, R4, R5, R6, R7, R8, R> joinWith(Model<R> model,
                                                                                    Value.BooleanValue condition) {
                return (Joined11<T, R0, R1, R2, R3, R4, R5, R6, R7, R8, R>) status().joinWith(model, condition);
            }

            /// @see Joiner#joinTo(Model, Value.BooleanValue)
            @SuppressWarnings("unchecked")
            default <R> Joined11<T, R0, R1, R2, R3, R4, R5, R6, R7, R8, R> joinTo(Model<R> model,
                                                                                  Value.BooleanValue condition) {
                return (Joined11<T, R0, R1, R2, R3, R4, R5, R6, R7, R8, R>) status().joinTo(model, condition);
            }
        }

        non-sealed interface Joined11<T, R0, R1, R2, R3, R4, R5, R6, R7, R8, R9> extends Stages,
                Joined<Tuple11<T, R0, R1, R2, R3, R4, R5, R6, R7, R8, R9>> {
            /// @see Joiner#join(Model, Value.BooleanValue)
            @SuppressWarnings("unchecked")
            default <R> Joined12<T, R0, R1, R2, R3, R4, R5, R6, R7, R8, R9, R> join(Model<R> model,
                                                                                    Value.BooleanValue condition) {
                return (Joined12<T, R0, R1, R2, R3, R4, R5, R6, R7, R8, R9, R>) status().join(model, condition);
            }

            /// @see Joiner#joinWith(Model, Value.BooleanValue)
            @SuppressWarnings("unchecked")
            default <R> Joined12<T, R0, R1, R2, R3, R4, R5, R6, R7, R8, R9, R> joinWith(Model<R> model,
                                                                                        Value.BooleanValue condition) {
                return (Joined12<T, R0, R1, R2, R3, R4, R5, R6, R7, R8, R9, R>) status().joinWith(model, condition);
            }

            /// @see Joiner#joinTo(Model, Value.BooleanValue)
            @SuppressWarnings("unchecked")
            default <R> Joined12<T, R0, R1, R2, R3, R4, R5, R6, R7, R8, R9, R> joinTo(Model<R> model,
                                                                                      Value.BooleanValue condition) {
                return (Joined12<T, R0, R1, R2, R3, R4, R5, R6, R7, R8, R9, R>) status().joinTo(model, condition);
            }
        }

        non-sealed interface Joined12<T, R0, R1, R2, R3, R4, R5, R6, R7, R8, R9, R10> extends Stages,
                Joined<Tuple12<T, R0, R1, R2, R3, R4, R5, R6, R7, R8, R9, R10>> {
            /// @see Joiner#join(Model, Value.BooleanValue)
            @SuppressWarnings("unchecked")
            default <R> Joined13<T, R0, R1, R2, R3, R4, R5, R6, R7, R8, R9, R10, R> join(Model<R> model,
                                                                                         Value.BooleanValue condition) {
                return (Joined13<T, R0, R1, R2, R3, R4, R5, R6, R7, R8, R9, R10, R>) status().join(model, condition);
            }

            /// @see Joiner#joinWith(Model, Value.BooleanValue)
            @SuppressWarnings("unchecked")
            default <R> Joined13<T, R0, R1, R2, R3, R4, R5, R6, R7, R8, R9, R10, R> joinWith(Model<R> model,
                                                                                             Value.BooleanValue condition) {
                return (Joined13<T, R0, R1, R2, R3, R4, R5, R6, R7, R8, R9, R10, R>) status().joinWith(model,
                        condition);
            }

            /// @see Joiner#joinTo(Model, Value.BooleanValue)
            @SuppressWarnings("unchecked")
            default <R> Joined13<T, R0, R1, R2, R3, R4, R5, R6, R7, R8, R9, R10, R> joinTo(Model<R> model,
                                                                                           Value.BooleanValue condition) {
                return (Joined13<T, R0, R1, R2, R3, R4, R5, R6, R7, R8, R9, R10, R>) status().joinTo(model, condition);
            }
        }

        non-sealed interface Joined13<T, R0, R1, R2, R3, R4, R5, R6, R7, R8, R9, R10, R11> extends Stages,
                Joined<Tuple13<T, R0, R1, R2, R3, R4, R5, R6, R7, R8, R9, R10, R11>> {
            /// @see Joiner#join(Model, Value.BooleanValue)
            @SuppressWarnings("unchecked")
            default <R> Joined14<T, R0, R1, R2, R3, R4, R5, R6, R7, R8, R9, R10, R11, R> join(Model<R> model,
                                                                                              Value.BooleanValue condition) {
                return (Joined14<T, R0, R1, R2, R3, R4, R5, R6, R7, R8, R9, R10, R11, R>) status().join(model,
                        condition);
            }

            /// @see Joiner#joinWith(Model, Value.BooleanValue)
            @SuppressWarnings("unchecked")
            default <R> Joined14<T, R0, R1, R2, R3, R4, R5, R6, R7, R8, R9, R10, R11, R> joinWith(Model<R> model,
                                                                                                  Value.BooleanValue condition) {
                return (Joined14<T, R0, R1, R2, R3, R4, R5, R6, R7, R8, R9, R10, R11, R>) status().joinWith(model,
                        condition);
            }

            /// @see Joiner#joinTo(Model, Value.BooleanValue)
            @SuppressWarnings("unchecked")
            default <R> Joined14<T, R0, R1, R2, R3, R4, R5, R6, R7, R8, R9, R10, R11, R> joinTo(Model<R> model,
                                                                                                Value.BooleanValue condition) {
                return (Joined14<T, R0, R1, R2, R3, R4, R5, R6, R7, R8, R9, R10, R11, R>) status().joinTo(model,
                        condition);
            }
        }

        non-sealed interface Joined14<T, R0, R1, R2, R3, R4, R5, R6, R7, R8, R9, R10, R11, R12> extends Stages,
                Joined<Tuple14<T, R0, R1, R2, R3, R4, R5, R6, R7, R8, R9, R10, R11, R12>> {
            /// @see Joiner#join(Model, Value.BooleanValue)
            @SuppressWarnings("unchecked")
            default <R> Joined15<T, R0, R1, R2, R3, R4, R5, R6, R7, R8, R9, R10, R11, R12, R> join(Model<R> model,
                                                                                                   Value.BooleanValue condition) {
                return (Joined15<T, R0, R1, R2, R3, R4, R5, R6, R7, R8, R9, R10, R11, R12, R>) status().join(model,
                        condition);
            }

            /// @see Joiner#joinWith(Model, Value.BooleanValue)
            @SuppressWarnings("unchecked")
            default <R> Joined15<T, R0, R1, R2, R3, R4, R5, R6, R7, R8, R9, R10, R11, R12, R> joinWith(Model<R> model,
                                                                                                       Value.BooleanValue condition) {
                return (Joined15<T, R0, R1, R2, R3, R4, R5, R6, R7, R8, R9, R10, R11, R12, R>) status().joinWith(model,
                        condition);
            }

            /// @see Joiner#joinTo(Model, Value.BooleanValue)
            @SuppressWarnings("unchecked")
            default <R> Joined15<T, R0, R1, R2, R3, R4, R5, R6, R7, R8, R9, R10, R11, R12, R> joinTo(Model<R> model,
                                                                                                     Value.BooleanValue condition) {
                return (Joined15<T, R0, R1, R2, R3, R4, R5, R6, R7, R8, R9, R10, R11, R12, R>) status().joinTo(model,
                        condition);
            }
        }

        non-sealed interface Joined15<T, R0, R1, R2, R3, R4, R5, R6, R7, R8, R9, R10, R11, R12, R13> extends Stages,
                Joined<Tuple15<T, R0, R1, R2, R3, R4, R5, R6, R7, R8, R9, R10, R11, R12, R13>> {
            /// @see Joiner#join(Model, Value.BooleanValue)
            @SuppressWarnings("unchecked")
            default <R> Joined16<T, R0, R1, R2, R3, R4, R5, R6, R7, R8, R9, R10, R11, R12, R13, R> join(Model<R> model,
                                                                                                        Value.BooleanValue condition) {
                return (Joined16<T, R0, R1, R2, R3, R4, R5, R6, R7, R8, R9, R10, R11, R12, R13, R>) status().join(model,
                        condition);
            }

            /// @see Joiner#joinWith(Model, Value.BooleanValue)
            @SuppressWarnings("unchecked")
            default <R> Joined16<T, R0, R1, R2, R3, R4, R5, R6, R7, R8, R9, R10, R11, R12, R13, R> joinWith(
                    Model<R> model, Value.BooleanValue condition) {
                return (Joined16<T, R0, R1, R2, R3, R4, R5, R6, R7, R8, R9, R10, R11, R12, R13, R>) status().joinWith(
                        model, condition);
            }

            /// @see Joiner#joinTo(Model, Value.BooleanValue)
            @SuppressWarnings("unchecked")
            default <R> Joined16<T, R0, R1, R2, R3, R4, R5, R6, R7, R8, R9, R10, R11, R12, R13, R> joinTo(
                    Model<R> model, Value.BooleanValue condition) {
                return (Joined16<T, R0, R1, R2, R3, R4, R5, R6, R7, R8, R9, R10, R11, R12, R13, R>) status().joinTo(
                        model, condition);
            }
        }

        non-sealed interface Joined16<T, R0, R1, R2, R3, R4, R5, R6, R7, R8, R9, R10, R11, R12, R13, R14> extends
                Stages,
                Joined<Tuple16<T, R0, R1, R2, R3, R4, R5, R6, R7, R8, R9, R10, R11, R12, R13, R14>> {

        }


        //endregion
      /*  static void main(String[] args) {
            java.awt.Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new java.awt.datatransfer.StringSelection(org.jooq.lambda.Seq.range(2, 17)
                    .map(n -> """
                              non-sealed  interface Joined%1$d<T,%3$s> extends Stages,Joined<Tuple%1$d<T,%3$s>>{
                                /// @see Joiner#join(Model, Value.BooleanValue)
                                 @SuppressWarnings("unchecked")
                                default <R> Joined%2$d<T,%3$s,R> join(Model<R> model, Value.BooleanValue condition){
                                    return (Joined%2$d<T,%3$s,R>)status().join(model, condition);
                                }
                                /// @see Joiner#joinWith(Model, Value.BooleanValue)
                                 @SuppressWarnings("unchecked")
                                default <R> Joined%2$d<T,%3$s,R> joinWith(Model<R> model, Value.BooleanValue condition){
                                      return (Joined%2$d<T,%3$s,R>)status().joinWith(model, condition);
                                }
                                /// @see Joiner#joinTo(Model, Value.BooleanValue)
                                 @SuppressWarnings("unchecked")
                               default <R> Joined%2$d<T,%3$s,R> joinTo(Model<R> model, Value.BooleanValue condition){
                                      return (Joined%2$d<T,%3$s,R>)status().joinTo(model, condition);
                                }
                            }
                            """.formatted(
                            n
                            , n + 1
                            , org.jooq.lambda.Seq.range(0, n-1).map("R%d"::formatted).toString(",")
                    )).toString("\n")), null);
        }*/
    }

    non-sealed interface Filter<T> extends Stages, Grouping<T> {
        @SuppressWarnings("unchecked")
        default Filtered<T> filter(Value.BooleanValue condition) {
            return (Filtered<T>) status().filter(condition);
        }
    }

    non-sealed interface Filtered<T> extends Stages, Grouping<T>, Modified<T>, Removed<T> {
    }

    non-sealed interface Grouping<T> extends Stages, Sorter<T> {
        @SuppressWarnings("unchecked")
        default Grouped<T> group(Field<?> field, Field<?>... extra) {
            return (Grouped<T>) status().grouped(field, extra);
        }
    }

    non-sealed interface Grouped<T> extends Stages, Sorter<T> {
        @SuppressWarnings("unchecked")
        default FilterGroup<T> having(Value.BooleanValue condition) {
            return (FilterGroup<T>) status().having(condition);
        }
    }

    non-sealed interface FilterGroup<T> extends Stages, Sorter<T> {
    }

    non-sealed interface Sorter<T> extends Stages, Picker<T> {
        @SuppressWarnings("unchecked")
        default Sorted<T> sort(StmtOrder order, StmtOrder... extra) {
            return (Sorted<T>) status().sorted(order, extra);
        }
    }

    non-sealed interface Sorted<T> extends Stages, Picker<T> {
    }


    non-sealed interface Picker<T> extends Stages, Querier<T> {
        @SuppressWarnings("unchecked")
        default <R0> Picked<R0> pick(Value<R0> field) {
            return (Picked<R0>) status().pick(field);
        }

        //region tuples
        @SuppressWarnings("unchecked")
        default <R0, R1> Picked<Tuple2<R0, R1>> pick(Value<R0> f0, Value<R1> f1) {
            return (Picked<Tuple2<R0, R1>>) status().pick(f0, f1);
        }

        @SuppressWarnings("unchecked")
        default <R0, R1, R2> Picked<Tuple3<R0, R1, R2>> pick(Value<R0> f0, Value<R1> f1, Value<R2> f2) {
            return (Picked<Tuple3<R0, R1, R2>>) status().pick(f0, f1, f2);
        }

        @SuppressWarnings("unchecked")
        default <R0, R1, R2, R3> Picked<Tuple4<R0, R1, R2, R3>> pick(Value<R0> f0, Value<R1> f1, Value<R2> f2,
                                                                     Value<R3> f3) {
            return (Picked<Tuple4<R0, R1, R2, R3>>) status().pick(f0, f1, f2, f3);
        }

        @SuppressWarnings("unchecked")
        default <R0, R1, R2, R3, R4> Picked<Tuple5<R0, R1, R2, R3, R4>> pick(Value<R0> f0, Value<R1> f1, Value<R2> f2,
                                                                             Value<R3> f3, Value<R4> f4) {
            return (Picked<Tuple5<R0, R1, R2, R3, R4>>) status().pick(f0, f1, f2, f3, f4);
        }

        @SuppressWarnings("unchecked")
        default <R0, R1, R2, R3, R4, R5> Picked<Tuple6<R0, R1, R2, R3, R4, R5>> pick(Value<R0> f0, Value<R1> f1,
                                                                                     Value<R2> f2, Value<R3> f3,
                                                                                     Value<R4> f4, Value<R5> f5) {
            return (Picked<Tuple6<R0, R1, R2, R3, R4, R5>>) status().pick(f0, f1, f2, f3, f4, f5);
        }

        @SuppressWarnings("unchecked")
        default <R0, R1, R2, R3, R4, R5, R6> Picked<Tuple7<R0, R1, R2, R3, R4, R5, R6>> pick(Value<R0> f0, Value<R1> f1,
                                                                                             Value<R2> f2, Value<R3> f3,
                                                                                             Value<R4> f4, Value<R5> f5,
                                                                                             Value<R6> f6) {
            return (Picked<Tuple7<R0, R1, R2, R3, R4, R5, R6>>) status().pick(f0, f1, f2, f3, f4, f5, f6);
        }

        @SuppressWarnings("unchecked")
        default <R0, R1, R2, R3, R4, R5, R6, R7> Picked<Tuple8<R0, R1, R2, R3, R4, R5, R6, R7>> pick(Value<R0> f0,
                                                                                                     Value<R1> f1,
                                                                                                     Value<R2> f2,
                                                                                                     Value<R3> f3,
                                                                                                     Value<R4> f4,
                                                                                                     Value<R5> f5,
                                                                                                     Value<R6> f6,
                                                                                                     Value<R7> f7) {
            return (Picked<Tuple8<R0, R1, R2, R3, R4, R5, R6, R7>>) status().pick(f0, f1, f2, f3, f4, f5, f6, f7);
        }

        @SuppressWarnings("unchecked")
        default <R0, R1, R2, R3, R4, R5, R6, R7, R8> Picked<Tuple9<R0, R1, R2, R3, R4, R5, R6, R7, R8>> pick(
                Value<R0> f0, Value<R1> f1, Value<R2> f2, Value<R3> f3, Value<R4> f4, Value<R5> f5, Value<R6> f6,
                Value<R7> f7, Value<R8> f8) {
            return (Picked<Tuple9<R0, R1, R2, R3, R4, R5, R6, R7, R8>>) status().pick(f0, f1, f2, f3, f4, f5, f6, f7,
                    f8);
        }

        @SuppressWarnings("unchecked")
        default <R0, R1, R2, R3, R4, R5, R6, R7, R8, R9> Picked<Tuple10<R0, R1, R2, R3, R4, R5, R6, R7, R8, R9>> pick(
                Value<R0> f0, Value<R1> f1, Value<R2> f2, Value<R3> f3, Value<R4> f4, Value<R5> f5, Value<R6> f6,
                Value<R7> f7, Value<R8> f8, Value<R9> f9) {
            return (Picked<Tuple10<R0, R1, R2, R3, R4, R5, R6, R7, R8, R9>>) status().pick(f0, f1, f2, f3, f4, f5, f6,
                    f7, f8, f9);
        }

        @SuppressWarnings("unchecked")
        default <R0, R1, R2, R3, R4, R5, R6, R7, R8, R9, R10> Picked<Tuple11<R0, R1, R2, R3, R4, R5, R6, R7, R8, R9, R10>> pick(
                Value<R0> f0, Value<R1> f1, Value<R2> f2, Value<R3> f3, Value<R4> f4, Value<R5> f5, Value<R6> f6,
                Value<R7> f7, Value<R8> f8, Value<R9> f9, Value<R10> f10) {
            return (Picked<Tuple11<R0, R1, R2, R3, R4, R5, R6, R7, R8, R9, R10>>) status().pick(f0, f1, f2, f3, f4, f5,
                    f6, f7, f8, f9, f10);
        }

        @SuppressWarnings("unchecked")
        default <R0, R1, R2, R3, R4, R5, R6, R7, R8, R9, R10, R11> Picked<Tuple12<R0, R1, R2, R3, R4, R5, R6, R7, R8, R9, R10, R11>> pick(
                Value<R0> f0, Value<R1> f1, Value<R2> f2, Value<R3> f3, Value<R4> f4, Value<R5> f5, Value<R6> f6,
                Value<R7> f7, Value<R8> f8, Value<R9> f9, Value<R10> f10, Value<R11> f11) {
            return (Picked<Tuple12<R0, R1, R2, R3, R4, R5, R6, R7, R8, R9, R10, R11>>) status().pick(f0, f1, f2, f3, f4,
                    f5, f6, f7, f8, f9,
                    f10, f11);
        }

        @SuppressWarnings("unchecked")
        default <R0, R1, R2, R3, R4, R5, R6, R7, R8, R9, R10, R11, R12> Picked<Tuple13<R0, R1, R2, R3, R4, R5, R6, R7, R8, R9, R10, R11, R12>> pick(
                Value<R0> f0, Value<R1> f1, Value<R2> f2, Value<R3> f3, Value<R4> f4, Value<R5> f5, Value<R6> f6,
                Value<R7> f7, Value<R8> f8, Value<R9> f9, Value<R10> f10, Value<R11> f11, Value<R12> f12) {
            return (Picked<Tuple13<R0, R1, R2, R3, R4, R5, R6, R7, R8, R9, R10, R11, R12>>) status().pick(f0, f1, f2,
                    f3, f4, f5,
                    f6, f7, f8,
                    f9, f10, f11,
                    f12);
        }

        @SuppressWarnings("unchecked")
        default <R0, R1, R2, R3, R4, R5, R6, R7, R8, R9, R10, R11, R12, R13> Picked<Tuple14<R0, R1, R2, R3, R4, R5, R6, R7, R8, R9, R10, R11, R12, R13>> pick(
                Value<R0> f0, Value<R1> f1, Value<R2> f2, Value<R3> f3, Value<R4> f4, Value<R5> f5, Value<R6> f6,
                Value<R7> f7, Value<R8> f8, Value<R9> f9, Value<R10> f10, Value<R11> f11, Value<R12> f12,
                Value<R13> f13) {
            return (Picked<Tuple14<R0, R1, R2, R3, R4, R5, R6, R7, R8, R9, R10, R11, R12, R13>>) status().pick(f0, f1,
                    f2, f3,
                    f4, f5,
                    f6, f7,
                    f8, f9,
                    f10, f11,
                    f12,
                    f13);
        }

        @SuppressWarnings("unchecked")
        default <R0, R1, R2, R3, R4, R5, R6, R7, R8, R9, R10, R11, R12, R13, R14> Picked<Tuple15<R0, R1, R2, R3, R4, R5, R6, R7, R8, R9, R10, R11, R12, R13, R14>> pick(
                Value<R0> f0, Value<R1> f1, Value<R2> f2, Value<R3> f3, Value<R4> f4, Value<R5> f5, Value<R6> f6,
                Value<R7> f7, Value<R8> f8, Value<R9> f9, Value<R10> f10, Value<R11> f11, Value<R12> f12,
                Value<R13> f13, Value<R14> f14) {
            return (Picked<Tuple15<R0, R1, R2, R3, R4, R5, R6, R7, R8, R9, R10, R11, R12, R13, R14>>) status().pick(f0,
                    f1,
                    f2,
                    f3,
                    f4,
                    f5,
                    f6,
                    f7,
                    f8,
                    f9,
                    f10,
                    f11,
                    f12,
                    f13,
                    f14);
        }

        @SuppressWarnings("unchecked")
        default <R0, R1, R2, R3, R4, R5, R6, R7, R8, R9, R10, R11, R12, R13, R14, R15> Picked<Tuple16<R0, R1, R2, R3, R4, R5, R6, R7, R8, R9, R10, R11, R12, R13, R14, R15>> pick(
                Value<R0> f0, Value<R1> f1, Value<R2> f2, Value<R3> f3, Value<R4> f4, Value<R5> f5, Value<R6> f6,
                Value<R7> f7, Value<R8> f8, Value<R9> f9, Value<R10> f10, Value<R11> f11, Value<R12> f12,
                Value<R13> f13, Value<R14> f14, Value<R15> f15) {
            return (Picked<Tuple16<R0, R1, R2, R3, R4, R5, R6, R7, R8, R9, R10, R11, R12, R13, R14, R15>>) status().pick(
                    f0, f1, f2, f3, f4, f5, f6, f7, f8, f9, f10, f11, f12, f13, f14, f15);
        }

        //endregion
/*
        static void main(String[] args) {
            java.awt.Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new java.awt.datatransfer.StringSelection(org.jooq.lambda.Seq.range(2, 17)
                    .map(n -> """
                             @SuppressWarnings("unchecked")
                            default <%1$s> Picked<Tuple%2$d<%1$S>> pick(%3$s){
                                return (Picked<Tuple%2$d<%1$S>>)status().pick(%4$s);
                            }
                            """.formatted(
                            org.jooq.lambda.Seq.range(0, n).map("R%d"::formatted).toString(",")
                            , n
                            , org.jooq.lambda.Seq.range(0, n).map("Field<R%1$s> f%1$d "::formatted).toString(",")
                            ,org.jooq.lambda.Seq.range(0, n).map("f%1$d "::formatted).toString(",")

                    )).toString("\n")), null);
        }*/
    }

    non-sealed interface Picked<T> extends Stages, Querier<T> {
        default Future<List<T>> top(int limit) {
            return status().top(limit);
        }
    }

    non-sealed interface Querier<T> extends Stages {
        default Future<Integer> count() {
            return status().count();
        }

        default Future<T> one() {
            return status().one();
        }
        default Future<Optional<T>> maybe() {
            return status().<T>first().map(Optional::ofNullable);
        }

        default Future<List<T>> any() {
            return status().any();
        }

        default Future<List<T>> slice(int skip, int maximum) {
            return status().slice(skip, maximum);
        }

        default <R> Future<R> one(Function<JsonObject, R> mapper) {
            return status().one(mapper);
        }
        default <R> Future<Optional<R>> maybe(Function<JsonObject, R> mapper) {
            return status().first(mapper).map(Optional::ofNullable);
        }

        default <R> Future<List<R>> any(Function<JsonObject, R> mapper) {
            return status().any(mapper);
        }
    }
}


