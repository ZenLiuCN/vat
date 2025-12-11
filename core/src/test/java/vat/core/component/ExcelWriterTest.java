package vat.core.component;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import vat.api.Data;
import vat.api.utils.Fn;

import java.util.List;

@Slf4j
@ExtendWith(VertxExtension.class)
class ExcelWriterTest {
    record Sample(String name, String phone, int age) implements Data {
        public Sample(JsonObject j) {
            this(j.getString("name"), j.getString("phone"), j.getInteger("age"));
        }

        @Override
        public JsonObject asJson() {
            return JsonObject.of(
                    "name", name,
                    "phone", phone,
                    "age", age
            );
        }
    }

    @Test
    void sheet(Vertx vertx, VertxTestContext vtc) {
        var sh = Excels.ExcelWriter.sheet()
                .sheet("person")
                .index(-1)
                .title(true)
                .style(s -> s.table(Excels.TableStyle.DarkBlackDarkFill).rowHeight(0, 0))
                .property(() -> Excels.ExcelWriter.STRING_PROPERTY.apply("姓名", "name", 0))
                .property(() -> Excels.ExcelWriter.STRING_PROPERTY.apply("手机", "phone", 1))
                .property(() -> Excels.ExcelWriter.INTEGER_PROPERTY.apply("年龄", "age", 2))
                .build();
        sh.write(List.of(
                        new Sample("wan", "123332132", 1),
                        new Sample("wan2", "123332132", 1),
                        new Sample("wan3", "123332132", 1),
                        new Sample("wan4", "123332132", 1),
                        new Sample("wan5", "123332132", 1),
                        new Sample("wan6", "123332132", 1)
                ))
                .flatMap(b -> vertx.fileSystem().writeFile("sample.xlsx", b))
                .onComplete(vtc.succeedingThenComplete());
    }

    @Test
    void sheets(Vertx vertx, VertxTestContext vtc) {
        Excels.ExcelWriter.CellStyler ss = (c, ws, p, d) -> c.borderColor("red");
        var sh = Excels.ExcelData.entity(Sample::new)
                .sheet("person")
                .index(-1)
                .title(true)
                .skipRows(1)
                .style(s -> s
                        .table(Excels.TableStyle.DarkBlackDarkFill)
                        .rowHeight(12.5, 0)
                        .columnWidth(i -> i == 1 ? 25.5 : 0)
                        .cellStyle((r, c) -> r > 0 && c == 1 ? ss : null)
                )
                .property(() -> Excels.ExcelData.STRING_PROPERTY.apply("姓名", "name", 0))
                .property(() -> Excels.ExcelData.STRING_PROPERTY.apply("手机", "phone", 1))
                .property(() -> Excels.ExcelData.INTEGER_PROPERTY.apply("年龄", "age", 2))
                .build();
        sh.write(List.of(
                        new Sample("wan", "123332132", 1),
                        new Sample("wan2", "123332132", 1),
                        new Sample("wan3", "123332132", 1),
                        new Sample("wan4", "123332132", 1),
                        new Sample("wan5", "123332132", 1),
                        new Sample("wan6", "123332132", 1)
                ))
                .flatMap(b -> vertx.fileSystem().writeFile("sample.xlsx", b))
                .flatMap(b -> vertx.fileSystem().readFile("sample.xlsx"))
                .flatMap(sh::read)
                .map(Fn.peek(s -> {
                    log.info("{}", s);
                }))
                .onComplete(vtc.succeedingThenComplete());
    }
}
