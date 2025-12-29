package vat.core.component;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import lombok.Builder;
import lombok.SneakyThrows;
import lombok.With;
import org.dhatim.fastexcel.ColumnStyleSetter;
import org.dhatim.fastexcel.StyleSetter;
import org.dhatim.fastexcel.Workbook;
import org.dhatim.fastexcel.Worksheet;
import org.dhatim.fastexcel.reader.ReadableWorkbook;
import org.dhatim.fastexcel.reader.Row;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.NullUnmarked;
import org.jspecify.annotations.Nullable;
import vat.api.Data;
import vat.api.implement.Codec;
import vat.api.implement.CommonCodec;
import vat.api.utils.BufferInputStream;
import vat.api.utils.BufferOutputStream;
import vat.api.utils.Fn;
import vat.api.utils.ITimes;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.*;
import java.util.stream.IntStream;

///
/// @author Zen.Liu
/// @since 2025-11-17
@SuppressWarnings("unused")

public interface Excels {
    String MIME_OPEN_EXCEL = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    enum TableStyle {

        MediumOrangeStrip("TableStyleMedium28"),
        MediumCyanStrip("TableStyleMedium27"),
        MediumPurpleStrip("TableStyleMedium26"),
        MediumGreenStrip("TableStyleMedium25"),
        MediumRedStrip("TableStyleMedium24"),
        MediumBlueStrip("TableStyleMedium23"),
        MediumGrayStrip("TableStyleMedium22"),
        MediumOrangeGrayStrip("TableStyleMedium21"),
        MediumCyanGrayStrip("TableStyleMedium20"),
        MediumPurpleGrayStrip("TableStyleMedium19"),
        MediumGreenGrayStrip("TableStyleMedium18"),
        MediumRedGrayStrip("TableStyleMedium17"),
        MediumBlueGrayStrip("TableStyleMedium16"),
        MediumBlackGrayStrip("TableStyleMedium15"),
        MediumOrange("TableStyleMedium14"),
        MediumCyan("TableStyleMedium13"),
        MediumPurple("TableStyleMedium12"),
        MediumGreen("TableStyleMedium11"),
        MediumRed("TableStyleMedium10"),
        MediumBlue("TableStyleMedium9"),
        MediumBlack("TableStyleMedium8"),
        MediumOrangeTransparent("TableStyleMedium7"),
        MediumCyanTransparent("TableStyleMedium6"),
        MediumPurpleTransparent("TableStyleMedium5"),
        MediumGreenTransparent("TableStyleMedium4"),
        MediumRedTransparent("TableStyleMedium3"),
        MediumBlueTransparent("TableStyleMedium2"),
        MediumBlackTransparent("TableStyleMedium1"),

        LightOrangeLightWhiteTitle("TableStyleLight21"),
        LightCyanLightWhiteTitle("TableStyleLight20"),
        LightPurpleLightWhiteTitle("TableStyleLight19"),
        LightGreenLightWhiteTitle("TableStyleLight18"),
        LightRedLightWhiteTitle("TableStyleLight17"),
        LightBlueLightWhiteTitle("TableStyleLight16"),
        LightBlackLightWhiteTitle("TableStyleLight15"),
        LightOrangeLight("TableStyleLight14"),
        LightCyanLight("TableStyleLight13"),
        LightPurpleLight("TableStyleLight12"),
        LightGreenLight("TableStyleLight11"),
        LightRedLight("TableStyleLight10"),
        LightBlueLight("TableStyleLight9"),
        LightBlackLight("TableStyleLight8"),
        LightOrangeLightStrip("TableStyleLight7"),
        LightCyanLightStrip("TableStyleLight6"),
        LightPurpleLightStrip("TableStyleLight5"),
        LightGreenLightStrip("TableStyleLight4"),
        LightRedLightStrip("TableStyleLight3"),
        LightBlueLightStrip("TableStyleLight2"),
        LightBlackLightStrip("TableStyleLight1"),

        DarkOrangeDark("TableStyleDark14"),
        DarkCyanDark("TableStyleDark13"),
        DarkPurpleDark("TableStyleDark12"),
        DarkGreenDark("TableStyleDark11"),
        DarkRedDark("TableStyleDark10"),
        DarkBlueDark("TableStyleDark9"),
        DarkBlackDark("TableStyleDark8"),
        DarkOrangeDarkFill("TableStyleDark7"),
        DarkCyanDarkFill("TableStyleDark6"),
        DarkPurpleDarkFill("TableStyleDark5"),
        DarkGreenDarkFill("TableStyleDark4"),
        DarkRedDarkFill("TableStyleDark3"),
        DarkBlueDarkFill("TableStyleDark2"),
        DarkBlackDarkFill("TableStyleDark1"),
        ;
        public final String code;

        TableStyle(String code) {
            this.code = code;
        }

        public static TableStyle of(int ordinal) {
            try {
                return values()[ordinal];
            } catch (Exception e) {
                throw new IllegalArgumentException("invalid ordinal " + ordinal + " of TableStyle");
            }
        }
    }

    enum HorizontalAlign {
        General,//	General Horizontal Alignment. When the item is serialized out as xml, its value is "general".
        Left,//	Left Horizontal Alignment. When the item is serialized out as xml, its value is "left".
        Center,//	Centered Horizontal Alignment. When the item is serialized out as xml, its value is "center".
        Right,//	Right Horizontal Alignment. When the item is serialized out as xml, its value is "right".
        Fill,//	Fill. When the item is serialized out as xml, its value is "fill".
        Justify,//	Justify. When the item is serialized out as xml, its value is "justify".
        CenterContinuous,//	Center Continuous Horizontal Alignment. When the item is serialized out as xml, its value is "centerContinuous".
        Distributed,//	Distributed Horizontal Alignment. When the item is serialized out as xml, its value is "distributed".
    }

    enum VerticalAlign {
        Top,//	Align Top. When the item is serialized out as xml, its value is "top".
        Center,//	Centered Vertical Alignment. When the item is serialized out as xml, its value is "center".
        Bottom,//	Aligned To Bottom. When the item is serialized out as xml, its value is "bottom".
        Justify,//	Justified Vertically. When the item is serialized out as xml, its value is "justify".
        Distributed,//	Distributed Vertical Alignment. When the item is serialized out as xml, its value is "distributed".
    }


    /// Both FastExcel and FastExcel reader are required.

    interface ExcelReader<T> {
        Optional<T> get(Row row, int col);

        default <R> ExcelReader<R> mapReader(Function<T, R> mapper) {
            return (r, i) -> get(r, i).map(mapper);
        }

        default ExcelReader<T> filter(Predicate<T> filter) {
            return (r, i) -> get(r, i).filter(filter);
        }

        interface PropertyData<P> extends ExcelReader<P> {
            String name();

            int index();

            ExcelReader<P> reader();

            Codec.DataProperty<P> codec();

            P defaultValue();

            @Override
            default Optional<P> get(Row row, int col) {
                return reader().get(row, col);
            }

            default void read(JsonObject o, Row r) {
                codec().set(o, name(), reader().get(r, index()).orElse(defaultValue()));
            }
        }

        interface SheetData<T extends Data> {
            String sheet();

            int index();

            int skipRows();

            List<? extends PropertyData<?>> properties();

            Function<JsonObject, T> creator();

            default Future<List<T>> read(Buffer buf) {
                return Future.future(p -> p.complete(readData(buf)));
            }

            default Future<List<T>> read(Vertx vertx, Buffer buf) {
                return vertx.executeBlocking(() -> readData(buf));
            }

            @SneakyThrows
            default List<T> readData(Buffer buf) {
                try (var is = new BufferInputStream(buf); var wb = new ReadableWorkbook(is)) {
                    var index = index();
                    var name = sheet();
                    var title = skipRows();
                    var bk = index >= 0
                            ? wb.getSheet(index).orElse(null)
                            : name.isEmpty() ? wb.getFirstSheet()
                            : wb.getSheets().filter(x -> x.getName().equals(name)).findFirst().orElse(null);
                    if (bk == null) return List.of();
                    try (var sm = bk.openStream()) {
                        return (title > 0 ? sm.skip(title) : sm).map(r -> {
                            var o = new JsonObject();
                            for (var p : properties()) {
                                p.read(o, r);
                            }
                            return creator().apply(o);
                        }).toList();
                    }
                }
            }
        }


        ExcelReader<String> RAW = Row::getCellRawValue;
        ExcelReader<String> STRING = Row::getCellAsString;

        static boolean parseBoolean(@Nullable String v) {
            if (v == null || v.isBlank()) return false;
            v = v.trim();
            if (v.equalsIgnoreCase("true")) return true;
            if (v.equalsIgnoreCase("on")) return true;
            if (v.equalsIgnoreCase("yes")) return true;
            return v.equalsIgnoreCase("是");
        }

        ExcelReader<Boolean> BOOLEAN = (r, i) -> r
                .getOptionalCell(i)
                .map(c -> switch (c.getType()) {
                    case NUMBER -> c.asNumber().intValue() > 0;
                    case STRING ->
                            Optional.of(c.asString()).filter(Predicate.not(String::isBlank)).map(ExcelReader::parseBoolean).orElse(null);
                    case EMPTY -> null;
                    default -> throw new IllegalStateException("invalid cell value:" + c);
                });
        ExcelReader<BigDecimal> DECIMAL = (r, i) -> r
                .getOptionalCell(i)
                .map(c -> switch (c.getType()) {
                    case NUMBER -> c.asNumber();
                    case STRING ->
                            Optional.of(c.asString()).filter(Predicate.not(String::isBlank)).map(BigDecimal::new).orElse(null);
                    case EMPTY -> null;
                    default -> throw new IllegalStateException("invalid cell value:" + c);
                });
        ExcelReader<LocalDateTime> DATETIME = Row::getCellAsDate;
        ExcelReader<Instant> INSTANT = RAW.filter(Predicate.not(String::isBlank)).mapReader(Instant::parse);
        ExcelReader<Byte> BYTE = DECIMAL.mapReader(BigDecimal::byteValue);
        ExcelReader<Short> SHORT = DECIMAL.mapReader(BigDecimal::shortValue);
        ExcelReader<Integer> INTEGER = DECIMAL.mapReader(BigDecimal::intValue);
        ExcelReader<Long> LONG = DECIMAL.mapReader(BigDecimal::longValue);
        ExcelReader<Float> FLOAT = DECIMAL.mapReader(BigDecimal::floatValue);
        ExcelReader<Double> DOUBLE = DECIMAL.mapReader(BigDecimal::doubleValue);
        ExcelReader<String> FORMULA = Row::getCellRawValue;
        ExcelReader<LocalDate> DATE = DATETIME.mapReader(LocalDateTime::toLocalDate);
        ExcelReader<OffsetDateTime> DATETIME_OF = INSTANT.mapReader(i -> i.atZone(ITimes.ZONE.get()).toOffsetDateTime());

        /// read sheet to list of object
        ///
        /// @param sheet      the sheet name or empty
        /// @param index      the sheet index or -1
        /// @param skipRows   title rows size or 0
        /// @param properties reader properties
        /// @param creator    creator of output object
        @Builder
        record SheetReader<T extends Data>(
                @With String sheet,
                @With int index,
                @With int skipRows,
                 List<Property<?>> properties,
                Function<JsonObject, T> creator) implements SheetData<T> {
            @NullUnmarked
            public static class SheetReaderBuilder<T extends Data> {
                public SheetReaderBuilder<T> property(UnaryOperator<Property.PropertyBuilder<?>> prop) {
                    var op = new ArrayList<>(properties == null ? List.of() : properties);
                    op.add(prop.apply(Property.builder().index(properties.size())).build());
                    return properties(op);
                }
            }
        }

        static <T extends Data> SheetReader.SheetReaderBuilder<T> builder(Function<JsonObject, T> creator) {
            return SheetReader.<T>builder().creator(creator);
        }

        /// @param <T>          property type
        /// @param name         the object property name
        /// @param index        of column
        /// @param reader       of column
        /// @param codec        of property
        /// @param defaultValue optional default value
        @Builder
        record Property<T>(
                @With String name,
                @With int index,
                ExcelReader<T> reader,
                Codec.DataProperty<T> codec,
                @Nullable @With T defaultValue
        ) implements PropertyData<T> {

            public static class PropertyBuilder<T> {
                public PropertyBuilder<T> of(String name, int index, ExcelReader<T> reader,
                                             Codec.DataProperty<T> codec) {
                    return name(name).index(index).reader(reader).codec(codec);
                }
            }
        }

        interface PropertyFactory<T> {
            Property<T> apply(String name, int index, @Nullable T defaultValue);

            default Property<T> apply(String name, int index) {
                return apply(name, index, null);
            }
        }

        PropertyFactory<String> STRING_PROPERTY = (c, n, i) -> new Property<>(c, n, STRING, Codec.STRING, i);
        PropertyFactory<String> FORMULA_PROPERTY = (c, n, i) -> new Property<>(c, n, FORMULA, Codec.STRING, i);
        PropertyFactory<Integer> INTEGER_PROPERTY = (c, n, i) -> new Property<>(c, n, INTEGER, Codec.INTEGER, i);
        PropertyFactory<Long> LONG_PROPERTY = (c, n, i) -> new Property<>(c, n, LONG, Codec.LONG, i);
        PropertyFactory<Float> FLOAT_PROPERTY = (c, n, i) -> new Property<>(c, n, FLOAT, Codec.FLOAT, i);
        PropertyFactory<Double> DOUBLE_PROPERTY = (c, n, i) -> new Property<>(c, n, DOUBLE, Codec.DOUBLE, i);
        PropertyFactory<Short> SHORT_PROPERTY = (c, n, i) -> new Property<>(c, n, SHORT, Codec.SHORT, i);
        PropertyFactory<Byte> BYTE_PROPERTY = (c, n, i) -> new Property<>(c, n, BYTE, Codec.BYTE, i);
        PropertyFactory<BigDecimal> DECIMAL_PROPERTY = (c, n, i) -> new Property<>(c, n, DECIMAL, CommonCodec.JAVA_MATH__BIG_DECIMAL, i);
        PropertyFactory<Boolean> BOOLEAN_PROPERTY = (c, n, i) -> new Property<>(c, n, BOOLEAN, Codec.BOOLEAN, i);
        PropertyFactory<LocalDate> DATE_PROPERTY = (c, n, i) -> new Property<>(c, n, DATE, CommonCodec.JAVA_TIME__LOCAL_DATE, i);
        PropertyFactory<LocalDateTime> DATETIME_PROPERTY = (c, n, i) -> new Property<>(c, n, DATETIME, CommonCodec.JAVA_TIME__LOCAL_DATE_TIME, i);
        PropertyFactory<OffsetDateTime> DATETIME_TZ_PROPERTY = (c, n, i) -> new Property<>(c, n, DATETIME_OF, CommonCodec.JAVA_TIME__OFFSET_DATE_TIME, i);
        PropertyFactory<Instant> INSTANT_PROPERTY = (c, n, i) -> new Property<>(c, n, INSTANT, Codec.INSTANT, i);

    }

    /// FastExcel  required.
    interface ExcelWriter<T> {
        /// @param ws      worksheet
        /// @param rows    row size
        /// @param headers header of columns
        static void withTable(Worksheet ws, int rows, List<String> headers, TableStyle tableStyle) {
            ws.range(0, 0, rows, headers.size() - 1).createTable(headers.toArray(new String[0])).styleInfo().setStyleName(tableStyle.code);
        }

        void set(Worksheet sheet, int row, int column, @Nullable T v);

        default <R> ExcelWriter<R> mapWriter(Function<R, T> mapper) {
            return (s, r, i, v) -> {
                if (v == null) s.value(r, i, "");
                else set(s, r, i, mapper.apply(v));
            };
        }

        interface Styler {
            enum Stage {
                INVALID,
                BEFORE_WORKBOOK,
                BEFORE_WORKSHEET,
                BEFORE_ROW,
                AFTER_CELL,
                AFTER_ROW,
                AFTER_WORKSHEET,
                AFTER_WORKBOOK;

                public static Stage check(
                        @Nullable Workbook wb,
                        @Nullable Worksheet ws,
                        boolean wrote,
                        @Nullable SheetsData sheets,
                        @Nullable SheetData<?> sheet,
                        int size,
                        @Nullable PropertyData<?> property,
                        @Nullable Data data,
                        int row
                ) {
                    if (
                            wb != null &&
                            ws != null &&
                            sheets == null &&
                            sheet != null &&
                            size > 0 &&
                            property != null &&
                            data != null &&
                            row >= 0
                    ) return AFTER_CELL;
                    if (
                            wb != null &&
                            ws != null &&
                            sheet != null &&
                            size > 0 &&
                            property == null &&
                            data != null &&
                            row >= 0
                    ) return wrote ? AFTER_ROW : BEFORE_ROW;
                    if (
                            wb != null &&
                            ws != null &&
                            sheets == null &&
                            sheet != null &&
                            size > 0
                    ) return wrote ? AFTER_WORKSHEET : BEFORE_WORKSHEET;
                    if (wb != null && sheets != null)
                        return wrote ? AFTER_WORKBOOK : BEFORE_WORKBOOK;
                    return INVALID;
                }
            }

            /// @param wrote    sheet wrote or row wrote
            /// @param ws       worksheet, null when using by {@link Sheets}
            /// @param wb       workbook
            /// @param sheets   sheets when use Sheets to write book.  This is null when using by {@link SheetWriter}
            /// @param sheet    sheet when use SheetWriter to write sheet. This is null when using by {@link Sheets}
            /// @param size     value size or -1 when using by {@link Sheets}.
            /// @param property property value after write a property or null .
            /// @param row      row value after write a property or -1 .
            default void apply(Workbook wb,
                               @Nullable Worksheet ws,
                               boolean wrote,
                               @Nullable SheetsData sheets,
                               @Nullable SheetData<?> sheet,
                               int size,
                               @Nullable PropertyData<?> property,
                               @Nullable Data data,
                               int row) {
                apply(Stage.check(wb, ws, wrote, sheets, sheet, size, property, data, row), wb, ws, wrote, sheets, sheet, size, property, data, row);
            }

            void apply(Stage stage, Workbook wb,
                       @Nullable Worksheet ws,
                       boolean wrote,
                       @Nullable SheetsData sheets,
                       @Nullable SheetData<?> sheet,
                       int size,
                       @Nullable PropertyData<?> property,
                       @Nullable Data data,
                       int row);

            /// Apply before and after workbook write
            default void apply(Workbook wb,
                               boolean wrote,
                               SheetsData sheets
            ) {
                apply(wb, null, wrote, sheets, null, -1, null, null, -1);
            }

            /// Apply before and after worksheet write
            default void apply(
                    Workbook wb,
                    Worksheet ws,
                    boolean wrote,
                    SheetData<?> sheet,
                    int size
            ) {
                apply(wb, ws, wrote, null, sheet, size, null, null, -1);
            }

            /// Apply before and after worksheet row write
            default void apply(
                    Workbook wb,
                    Worksheet ws,
                    boolean wrote,
                    SheetData<?> sheet,
                    int size,
                    Data data,
                    int row
            ) {
                apply(wb, ws, wrote, null, sheet, size, null, data, row);
            }

            /// Apply b after worksheet row column write
            default void apply(
                    Workbook wb,
                    Worksheet ws,
                    SheetData<?> sheet,
                    int size,
                    PropertyData<?> property,
                    Data data,
                    int row
            ) {
                apply(wb, ws, true, null, sheet, size, property, data, row);
            }

            default Styler andThen(Styler next) {
                Objects.requireNonNull(next);
                return (stage, wb, ws, wrote, sheets, sheet, size, property, data, row) -> {
                    apply(stage, wb, ws, wrote, sheets, sheet, size, property, data, row);
                    next.apply(stage, wb, ws, wrote, sheets, sheet, size, property, data, row);
                };
            }

            default Styler compose(Styler before) {
                Objects.requireNonNull(before);
                return (stage, wb, ws, wrote, sheets, sheet, size, property, data, row) -> {
                    before.apply(stage, wb, ws, wrote, sheets, sheet, size, property, data, row);
                    apply(stage, wb, ws, wrote, sheets, sheet, size, property, data, row);
                };
            }
        }

        interface SheetsData {
            @Nullable
            Styler styler();

            List<? extends SheetData<?>> sheets();


            default Future<Buffer> write(List<List<?>> values) {
                return Future.future(p -> p.complete(writeData(values)));
            }

            default Future<Buffer> write(Vertx vertx, List<List<?>> values) {
                return vertx.executeBlocking(() -> writeData(values));
            }

            default Future<Buffer> write(Function<? super SheetData<?>, List<?>> valueProvider) {
                return Future.future(p -> p.complete(writeBufData(valueProvider)));
            }

            default Future<Buffer> write(Vertx vertx, Function<? super SheetData<?>, List<?>> valueProvider) {
                return vertx.executeBlocking(() -> writeBufData(valueProvider));
            }

            @SuppressWarnings({"rawtypes", "unchecked"})
            @SneakyThrows
            default Buffer writeBufData(Function<? super SheetData<?>, List<?>> values) {
                var out = Buffer.buffer();
                var styler = styler();
                try (var os = new BufferOutputStream(out); var wb = new Workbook(os, APPLICATION.get(), VERSION.get())) {
                    if (styler != null) styler.apply(wb, false, this);
                    for (var writer : sheets()) {
                        var d = values.apply(writer);
                        writer.writeData((List) d, wb);
                    }
                    if (styler != null) styler.apply(wb, true, this);
                    wb.finish();
                }
                return out;
            }

            @SuppressWarnings({"rawtypes", "unchecked"})
            default Future<Buffer> writeFuture(Vertx vertx, Function<SheetData<?>, Future<List<?>>> values) {
                var out = Buffer.buffer();
                var os = new BufferOutputStream(out);
                var wb = new Workbook(os, APPLICATION.get(), VERSION.get());
                return Fn.Many.Flat.seq(sheets()
                                .stream()
                                .<Future<Void>>map(x -> values
                                        .apply(x)
                                        .flatMap(vx -> x.writeData(vertx, (List) vx, wb))
                                )
                                .toList()
                        )
                        .<Void>mapEmpty()
                        .eventually(() -> Future.future(p -> {
                            closer(wb, os);
                            p.complete();
                        }))
                        .map(out)
                        ;
            }

            @SneakyThrows
            static void closer(Workbook wb, BufferOutputStream os) {
                wb.finish();
                wb.close();
                os.close();
            }

            @SuppressWarnings({"rawtypes", "unchecked"})
            @SneakyThrows
            default Buffer writeData(List<List<?>> values) {
                if (values.size() != sheets().size()) throw new IllegalStateException("value size not match sheets");
                var out = Buffer.buffer();
                var styler = styler();
                try (var os = new BufferOutputStream(out); var wb = new Workbook(os, APPLICATION.get(), VERSION.get())) {
                    if (styler != null) styler.apply(wb, false, this);
                    var dit = values.iterator();
                    for (var writer : sheets()) {
                        var d = dit.next();
                        writer.writeData((List) d, wb);
                    }
                    if (styler != null) styler.apply(wb, true, this);
                    wb.finish();
                }
                return out;
            }
        }

        interface SheetData<T extends Data> {
            String sheet();

            /// sheet index, -1 for use sheet name
            int index();

            boolean title();

            @Nullable
            Styler styler();

            List<? extends PropertyData<?>> properties();

            @SneakyThrows
            default Buffer writeData(List<? extends T> values) {
                var out = Buffer.buffer();
                try (var os = new BufferOutputStream(out); var wb = new Workbook(os, APPLICATION.get(), VERSION.get())) {
                    writeData(values, wb);
                    wb.finish();
                }
                return out;
            }

            default void writeData(List<? extends T> values, Workbook wb) {
                var st = wb.newWorksheet(sheet());
                var styler = styler();
                var r = 0;
                if (title()) {
                    for (var p : properties()) {
                        p.caption(st, r);
                    }
                    r++;
                }
                var n = values.size();
                if (styler != null) styler.apply(wb, st, false, this, n);
                for (var v : values) {
                    var j = v.asJson();
                    if (styler != null) styler.apply(wb, st, false, this, n, v, r);
                    for (var p : properties()) {
                        p.write(st, r, j);
                        if (styler != null) styler.apply(wb, st, this, n, p, v, r);
                    }
                    if (styler != null) styler.apply(wb, st, true, this, n, v, r);
                    r++;
                }
                if (styler != null) styler.apply(wb, st, true, this, n);

            }

            @SneakyThrows
            default Future<@Nullable Void> writeData(Vertx vertx, List<T> values, Workbook wb) {
                return vertx.executeBlocking(() -> {
                    writeData(values, wb);
                    return null;
                });
            }

            default Future<Buffer> write(List<T> values) {
                return Future.future(p -> p.complete(writeData(values)));
            }

            default Future<Buffer> write(Vertx vertx, List<T> values) {
                return vertx.executeBlocking(() -> writeData(values));
            }

            default List<String> headers() {
                return properties().stream().sorted(Comparator.comparingInt(PropertyData::index)).map(PropertyData::caption).toList();
            }
        }

        interface PropertyData<P> extends ExcelWriter<P> {
            /// property index .
            int index();

            /// caption of property
            String caption();

            /// property name
            String name();

            ExcelWriter<P> writer();

            Codec.DataProperty<P> codec();

            @Override
            default void set(Worksheet sheet, int row, int column, P v) {
                writer().set(sheet, row, column, v);
            }

            default void write(Worksheet s, int r, JsonObject v) {
                writer().set(s, r, index(), codec().get(v, name()));
            }

            default void caption(Worksheet s, int r) {
                s.value(r, index(), caption());
            }
        }


        /// write sheet from list of object
        ///
        /// @param sheet      the sheet name or empty
        /// @param title      write a title row
        /// @param styler     invoke after each row and all rows done
        /// @param properties write properties
        @Builder
        record SheetWriter<T extends Data>(
                @With String sheet,
                @With int index,
                @With boolean title,
                Styler styler,
                @Nullable List<Property<?>> properties) implements SheetData<T> {

            public static class SheetWriterBuilder<T extends Data> {
                public SheetWriterBuilder<T> property(UnaryOperator<Property.PropertyBuilder<?>> prop) {
                    var op = new ArrayList<>(properties == null ? List.of() : properties);
                    op.add(prop.apply(Property.builder().index(properties == null ? 0 : properties.size())).build());
                    return properties(op);
                }

                public SheetWriterBuilder<T> property(Supplier<Property<?>> prop) {
                    var op = new ArrayList<>(properties == null ? List.of() : properties);
                    op.add(prop.get());
                    return properties(op);
                }

                public SheetWriterBuilder<T> style(UnaryOperator<StylerBuilder> prop) {
                    return styler(prop.apply(ExcelWriter.styler()).build());
                }
            }


        }

        static <T extends Data> SheetWriter.SheetWriterBuilder<T> sheet() {
            return SheetWriter.builder();
        }

        /// @param styler invoke before and after all sheets wrote.
        @Builder
        record Sheets(
                Styler styler,
               List<SheetWriter<?>> sheets
        ) implements SheetsData {
            @NullUnmarked
            public static class SheetsBuilder {
                <T extends Data> SheetsBuilder sheet(UnaryOperator<SheetWriter.SheetWriterBuilder<@NonNull T>> prop) {
                    var op = new ArrayList<>(sheets == null ? List.of() : sheets);
                    op.add(prop.apply(SheetWriter.builder()).build());
                    return sheets(op);
                }

                SheetsBuilder style(UnaryOperator<StylerBuilder> prop) {
                    return styler(prop.apply(ExcelWriter.styler()).build());
                }
            }
        }

        static Sheets.SheetsBuilder sheets() {
            return Sheets.builder();
        }

        AtomicReference<String> APPLICATION = new AtomicReference<>("vat");
        AtomicReference<String> VERSION = new AtomicReference<>("1.0");


        record Property<T>(
                @With String caption,
                @With String name,
                @With int index,
                ExcelWriter<T> writer,
                Codec.DataProperty<T> codec
        ) implements PropertyData<T> {

            public static <T> PropertyBuilder<T> builder() {
                return new PropertyBuilder<>();
            }

            public static class PropertyBuilder<T> {
                @Nullable
                private String caption;
                @Nullable
                private String name;
                private int index;
                @Nullable
                private ExcelWriter<T> writer;
                private Codec.@Nullable DataProperty<T> codec;

                PropertyBuilder() {
                }

                public PropertyBuilder<T> caption(String caption) {
                    this.caption = caption;
                    return this;
                }

                public PropertyBuilder<T> name(String name) {
                    this.name = name;
                    return this;
                }

                public PropertyBuilder<T> index(int index) {
                    this.index = index;
                    return this;
                }

                public PropertyBuilder<T> writer(ExcelWriter<T> writer) {
                    this.writer = writer;
                    return this;
                }

                public PropertyBuilder<T> codec(Codec.DataProperty<T> codec) {
                    this.codec = codec;
                    return this;
                }

                public Property<T> build() {
                    assert this.caption != null : "caption required";
                    assert this.name != null : "name required";
                    assert this.writer != null : "writer required";
                    assert this.codec != null : "codec required";
                    return new Property<>(this.caption, this.name, this.index, this.writer, this.codec);
                }

                public String toString() {
                    return "Excels.ExcelWriter.Property.PropertyBuilder(caption=" + this.caption + ", name=" + this.name + ", index=" + this.index + ", writer=" + this.writer + ", codec=" + this.codec + ")";
                }
            }
        }

        enum BOOLEAN_TEXT {
            DEFAULT,
            ON_OFF,
            CHS,
            YES_NO,
        }

        AtomicReference<BOOLEAN_TEXT> BOOLEAN_TEXT_MODE = new AtomicReference<>(BOOLEAN_TEXT.DEFAULT);

        static String formatBoolean(@Nullable Boolean v) {
            return v == null ? "" : switch (BOOLEAN_TEXT_MODE.get()) {
                case ON_OFF -> v ? "on" : "off";
                case CHS -> v ? "是" : "否";
                case YES_NO -> v ? "yes" : "no";
                default -> v ? "true" : "false";
            };
        }

        ExcelWriter<String> STRING = Worksheet::value;
        ExcelWriter<String> FORMULA = Worksheet::formula;
        ExcelWriter<Boolean> BOOLEAN = (ws, r, c, v) -> ws.value(r, c, formatBoolean(v));
        ExcelWriter<Number> NUMBER = Worksheet::value;
        ExcelWriter<Integer> INTEGER = NUMBER.mapWriter(v -> v);
        ExcelWriter<Long> LONG = NUMBER.mapWriter(v -> v);
        ExcelWriter<Float> FLOAT = NUMBER.mapWriter(v -> v);
        ExcelWriter<Double> DOUBLE = NUMBER.mapWriter(v -> v);
        ExcelWriter<Short> SHORT = NUMBER.mapWriter(v -> v);
        ExcelWriter<Byte> BYTE = NUMBER.mapWriter(v -> v);
        ExcelWriter<BigDecimal> DECIMAL = NUMBER.mapWriter(v -> v);
        ExcelWriter<LocalDate> DATE = Worksheet::value;
        ExcelWriter<LocalDateTime> DATETIME = Worksheet::value;
        ExcelWriter<ZonedDateTime> DATETIME_TZ = Worksheet::value;
        ExcelWriter<OffsetDateTime> DATETIME_OF = DATETIME_TZ.mapWriter(OffsetDateTime::toZonedDateTime);
        ExcelWriter<Instant> INSTANT = DATETIME_TZ.mapWriter(o -> o.atZone(ITimes.ZONE.get()));

        interface PropertyFactory<T> {
            Property<T> apply(String caption, String name, int index);
        }

        PropertyFactory<String> STRING_PROPERTY = (c, n, i) -> new Property<>(c, n, i, STRING, Codec.STRING);
        PropertyFactory<String> FORMULA_PROPERTY = (c, n, i) -> new Property<>(c, n, i, FORMULA, Codec.STRING);
        PropertyFactory<Integer> INTEGER_PROPERTY = (c, n, i) -> new Property<>(c, n, i, INTEGER, Codec.INTEGER);
        PropertyFactory<Long> LONG_PROPERTY = (c, n, i) -> new Property<>(c, n, i, LONG, Codec.LONG);
        PropertyFactory<Float> FLOAT_PROPERTY = (c, n, i) -> new Property<>(c, n, i, FLOAT, Codec.FLOAT);
        PropertyFactory<Double> DOUBLE_PROPERTY = (c, n, i) -> new Property<>(c, n, i, DOUBLE, Codec.DOUBLE);
        PropertyFactory<Short> SHORT_PROPERTY = (c, n, i) -> new Property<>(c, n, i, SHORT, Codec.SHORT);
        PropertyFactory<Byte> BYTE_PROPERTY = (c, n, i) -> new Property<>(c, n, i, BYTE, Codec.BYTE);
        PropertyFactory<BigDecimal> DECIMAL_PROPERTY = (c, n, i) -> new Property<>(c, n, i, DECIMAL, CommonCodec.JAVA_MATH__BIG_DECIMAL);
        PropertyFactory<Boolean> BOOLEAN_PROPERTY = (c, n, i) -> new Property<>(c, n, i, BOOLEAN, Codec.BOOLEAN);
        PropertyFactory<LocalDate> DATE_PROPERTY = (c, n, i) -> new Property<>(c, n, i, DATE, CommonCodec.JAVA_TIME__LOCAL_DATE);
        PropertyFactory<LocalDateTime> DATETIME_PROPERTY = (c, n, i) -> new Property<>(c, n, i, DATETIME, CommonCodec.JAVA_TIME__LOCAL_DATE_TIME);
        PropertyFactory<OffsetDateTime> DATETIME_TZ_PROPERTY = (c, n, i) -> new Property<>(c, n, i, DATETIME_OF, CommonCodec.JAVA_TIME__OFFSET_DATE_TIME);
        PropertyFactory<Instant> INSTANT_PROPERTY = (c, n, i) -> new Property<>(c, n, i, INSTANT, Codec.INSTANT);

        class StylerBuilder {
            @Nullable
            private Styler s;

            /// create table style
            public StylerBuilder table(TableStyle style) {
                Styler x = (stage, wb, ws, wrote, sheets, sheet, size, property, data, row) -> {
                    if (stage == Styler.Stage.AFTER_WORKSHEET) {
                        assert ws != null;
                        assert sheet != null;
                        withTable(ws, size, sheet.headers(), style);
                    }
                };
                s = s == null ? x : s.andThen(x);
                return this;
            }

            /// config workbook properties
            public StylerBuilder workbookProperties(Consumer<Workbook> conf) {
                Objects.requireNonNull(conf);
                Styler x = (stage, wb, ws, wrote, sheets, sheet, size, property, data, row) -> {
                    if (stage == Styler.Stage.BEFORE_WORKBOOK) {
                        conf.accept(wb);
                    }
                };
                s = s == null ? x : s.andThen(x);
                return this;
            }

            /// config worksheet properties
            public StylerBuilder worksheetProperties(Consumer<Worksheet> conf) {
                Objects.requireNonNull(conf);
                Styler x = (stage, wb, ws, wrote, sheets, sheet, size, property, data, row) -> {
                    if (stage == Styler.Stage.BEFORE_WORKSHEET) {
                        assert ws != null;
                        conf.accept(ws);
                    }
                };
                s = s == null ? x : s.andThen(x);
                return this;
            }

            /// config row height
            public StylerBuilder rowHeight(double title, double values) {
                if (title <= 0 && values <= 0) return this;
                Styler x = (stage, wb, ws, wrote, sheets, sheet, size, property, data, row) -> {
                    if (stage == Styler.Stage.AFTER_WORKSHEET) {
                        assert ws != null;
                        assert sheet != null;
                        if (title > 0 && sheet.title()) {
                            ws.rowHeight(0, title);
                        }
                        if (values > 0) {
                            var s = sheet.title() ? 1 : 0;
                            ws.rowHeight(s, size);
                        }
                    }
                };
                s = s == null ? x : s.andThen(x);
                return this;
            }

            /// config column width
            public StylerBuilder columnWidth(IntToDoubleFunction decide) {
                Objects.requireNonNull(decide);
                var set = new boolean[]{false};
                Styler x = (stage, wb, ws, wrote, sheets, sheet, size, property, data, row) -> {
                    if (!set[0] && stage == Styler.Stage.AFTER_WORKSHEET) {
                        assert ws != null;
                        assert sheet != null;
                        var s = sheet.properties().stream().mapToInt(PropertyData::index).toArray();
                        var mi = IntStream.of(s).min().orElse(-1);
                        if (mi < 0) mi = 0;
                        var mx = IntStream.of(s).max().orElse(s.length - 1);
                        if (mx < 0) mx = s.length - 1;
                        for (int i = mi; i <= mx; i++) {
                            var xs = decide.applyAsDouble(i);
                            if (xs > 0) ws.width(i, xs);
                        }
                        set[0] = true;
                    }
                };
                s = s == null ? x : s.andThen(x);
                return this;
            }

            /// config column style
            public StylerBuilder columnStyle(IntFunction<@Nullable ColumnStyler> decide) {
                Objects.requireNonNull(decide);
                var set = new boolean[]{false};
                Styler x = (stage, wb, ws, wrote, sheets, sheet, size, property, data, row) -> {
                    if (!set[0] && stage == Styler.Stage.AFTER_WORKSHEET) {
                        assert ws != null;
                        assert sheet != null;
                        var s = sheet.properties().stream().mapToInt(PropertyData::index).toArray();
                        var mi = IntStream.of(s).min().orElse(-1);
                        if (mi < 0) mi = 0;
                        var mx = IntStream.of(s).max().orElse(s.length - 1);
                        if (mx < 0) mx = s.length - 1;
                        for (int i = mi; i <= mx; i++) {
                            var c = decide.apply(i);
                            if (c != null) c.apply(ws.style(i)).set();
                        }
                        set[0] = true;
                    }
                };
                s = s == null ? x : s.andThen(x);
                return this;
            }

            /// config cell style
            public StylerBuilder cellStyle(BiIntFunction<@Nullable CellStyler> decide) {
                Objects.requireNonNull(decide);
                Styler x = (stage, wb, ws, wrote, sheets, sheet, size, property, data, row) -> {
                    if (stage == Styler.Stage.AFTER_CELL) {
                        assert ws != null;
                        assert property != null;
                        assert data != null;
                        var c = decide.apply(row, property.index());
                        if (c != null) c.apply(ws.style(row, property.index()), ws, property, data).set();
                    }
                };
                s = s == null ? x : s.andThen(x);
                return this;
            }

            public Styler build() {
                return Objects.requireNonNull(s);
            }
        }

        interface BiIntFunction<T extends @Nullable Object> {
            T apply(int row, int col);
        }

        interface CellStyler {
            StyleSetter apply(StyleSetter setter, Worksheet sheet, PropertyData<?> prop, Data data);
        }

        interface ColumnStyler extends UnaryOperator<ColumnStyleSetter> {

        }

        static StylerBuilder styler() {
            return new StylerBuilder();
        }

    }

    /// Both FastExcel and FastExcel reader are required.
    interface ExcelData<T> extends ExcelWriter<T>, ExcelReader<T> {
        @Builder
        record Property<T>(
                @With String caption,
                @With String name,
                @With int index,
                ExcelWriter<T> writer,
                ExcelReader<T> reader,
                Codec.DataProperty<T> codec,
                @Nullable  @With T defaultValue
        ) implements ExcelData<T>, ExcelWriter.PropertyData<T>, ExcelReader.PropertyData<T> {
            public static class PropertyBuilder<T> {
                public ExcelData.Property<T> build() {
                    return new ExcelData.Property<>(
                            Fn.nonBlank(caption), Fn.nonBlank(name), index,
                            Objects.requireNonNull(writer, "missing  writer"),
                            Objects.requireNonNull(reader, "missing reader"),
                            Objects.requireNonNull(codec, "missing codec"), defaultValue);
                }
            }

        }

        @Builder
        record Entity<T extends Data>(
                @With String sheet,
                @With int index,
                @With int skipRows,
                @With boolean title,
                @With Styler styler,
                List<Property<?>> properties,
                Function<JsonObject, T> creator
        ) implements ExcelWriter.SheetData<T>, ExcelReader.SheetData<T> {
            @NullUnmarked
            public static class EntityBuilder<T extends Data> {
                public <P> EntityBuilder<T> property(UnaryOperator<Property.PropertyBuilder<@NonNull P>> make) {
                    var o = properties instanceof ArrayList<?> a ? properties : new ArrayList<>(properties == null ? List.of() : properties);
                    o.add(make.apply(Property.builder()).build());
                    return properties(o);
                }

                public <P> EntityBuilder<T> property(Supplier<Property<@NonNull P>> make) {
                    var o = properties instanceof ArrayList<?> a ? properties : new ArrayList<>(properties == null ? List.of() : properties);
                    o.add(make.get());
                    return properties(o);
                }

                EntityBuilder<T> style(UnaryOperator<StylerBuilder> prop) {
                    return styler(prop.apply(ExcelWriter.styler()).build());
                }

                public Entity<@NonNull T> build() {
                    if (skipRows == 0 && title) skipRows = 1;
                    return new Entity<>(sheet, index, skipRows, title, styler, Objects.requireNonNull(properties, "missing properties"), creator);
                }
            }
        }

        static <T extends Data> Entity.EntityBuilder<T> entity(Function<JsonObject, T> creator) {
            return Entity.<T>builder().creator(creator);
        }

        interface PropertyFactory<T> {
            Property<T> apply(String caption, String name, int index, @Nullable T defaultValue);

            default Property<T> apply(String caption, String name, int index) {
                return apply(caption, name, index, null);
            }
        }

        PropertyFactory<String> STRING_PROPERTY = (c, n, i, v) -> new Property<>(c, n, i, ExcelWriter.STRING, ExcelReader.STRING, Codec.STRING, v);
        PropertyFactory<String> FORMULA_PROPERTY = (c, n, i, v) -> new Property<>(c, n, i, ExcelWriter.FORMULA, ExcelReader.FORMULA, Codec.STRING, v);
        PropertyFactory<Integer> INTEGER_PROPERTY = (c, n, i, v) -> new Property<>(c, n, i, ExcelWriter.INTEGER, ExcelReader.INTEGER, Codec.INTEGER, v);
        PropertyFactory<Long> LONG_PROPERTY = (c, n, i, v) -> new Property<>(c, n, i, ExcelWriter.LONG, ExcelReader.LONG, Codec.LONG, v);
        PropertyFactory<Float> FLOAT_PROPERTY = (c, n, i, v) -> new Property<>(c, n, i, ExcelWriter.FLOAT, ExcelReader.FLOAT, Codec.FLOAT, v);
        PropertyFactory<Double> DOUBLE_PROPERTY = (c, n, i, v) -> new Property<>(c, n, i, ExcelWriter.DOUBLE, ExcelReader.DOUBLE, Codec.DOUBLE, v);
        PropertyFactory<Short> SHORT_PROPERTY = (c, n, i, v) -> new Property<>(c, n, i, ExcelWriter.SHORT, ExcelReader.SHORT, Codec.SHORT, v);
        PropertyFactory<Byte> BYTE_PROPERTY = (c, n, i, v) -> new Property<>(c, n, i, ExcelWriter.BYTE, ExcelReader.BYTE, Codec.BYTE, v);
        PropertyFactory<BigDecimal> DECIMAL_PROPERTY = (c, n, i, v) -> new Property<>(c, n, i, ExcelWriter.DECIMAL, ExcelReader.DECIMAL, CommonCodec.JAVA_MATH__BIG_DECIMAL, v);
        PropertyFactory<Boolean> BOOLEAN_PROPERTY = (c, n, i, v) -> new Property<>(c, n, i, ExcelWriter.BOOLEAN, ExcelReader.BOOLEAN, Codec.BOOLEAN, v);
        PropertyFactory<LocalDate> DATE_PROPERTY = (c, n, i, v) -> new Property<>(c, n, i, ExcelWriter.DATE, ExcelReader.DATE, CommonCodec.JAVA_TIME__LOCAL_DATE, v);
        PropertyFactory<LocalDateTime> DATETIME_PROPERTY = (c, n, i, v) -> new Property<>(c, n, i, ExcelWriter.DATETIME, ExcelReader.DATETIME, CommonCodec.JAVA_TIME__LOCAL_DATE_TIME, v);
        PropertyFactory<OffsetDateTime> DATETIME_TZ_PROPERTY = (c, n, i, v) -> new Property<>(c, n, i, ExcelWriter.DATETIME_OF, ExcelReader.DATETIME_OF, CommonCodec.JAVA_TIME__OFFSET_DATE_TIME, v);
        PropertyFactory<Instant> INSTANT_PROPERTY = (c, n, i, v) -> new Property<>(c, n, i, ExcelWriter.INSTANT, ExcelReader.INSTANT, Codec.INSTANT, v);
    }
}
