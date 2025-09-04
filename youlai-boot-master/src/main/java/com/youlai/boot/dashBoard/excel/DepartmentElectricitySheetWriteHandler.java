package com.youlai.boot.dashBoard.excel;

import cn.idev.excel.write.handler.SheetWriteHandler;
import cn.idev.excel.write.metadata.holder.WriteSheetHolder;
import cn.idev.excel.write.metadata.holder.WriteWorkbookHolder;
import cn.idev.excel.write.metadata.style.WriteCellStyle;
import cn.idev.excel.write.metadata.style.WriteFont;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;

import java.util.ArrayList;
import java.util.List;

/**
 * 部门电量汇总Excel写入处理器
 * 负责设置Excel的样式、标题、表头等
 *
 * @author way
 * @since 2025-09-02
 */
@Slf4j
public class DepartmentElectricitySheetWriteHandler implements SheetWriteHandler {
    private final String title;
    private final String[] header;

    /**
     * 构造函数，初始化部门电费表格写入处理器
     * @param title 表格的大标题
     * @param header 表格的列头数组
     */
    public DepartmentElectricitySheetWriteHandler(String title, String[] header) {
        this.title = title;
        this.header = header;
    }

    /**
     * 动态生成表头数据结构
     * @param header 列头数组
     * @return 包含表头信息的二维列表，每个子列表包含大标题和具体列头
     */
    public static List<List<String>> head(String firstTitle, String time, String[] header) {
        List<List<String>> list = new ArrayList<>();
        for (String head : header) {
            List<String> category = new ArrayList<>();
            category.add(firstTitle);
            category.add(time);
            category.add(head);
            list.add(category);
        }
        return list;
    }

    /**
     * 创建表头单元格样式
     * @return 配置好的表头单元格样式
     */
    public static WriteCellStyle getHeadStyle() {
        // 头的策略
        WriteCellStyle headWriteCellStyle = new WriteCellStyle();
        // 背景颜色
        headWriteCellStyle.setFillForegroundColor(IndexedColors.PINK.getIndex());
        headWriteCellStyle.setFillPatternType(FillPatternType.SOLID_FOREGROUND);
        // 字体
        WriteFont headWriteFont = new WriteFont();
        headWriteFont.setFontName("黑体");//设置字体名字
        headWriteFont.setFontHeightInPoints((short) 12);//设置字体大小
        headWriteFont.setBold(true);//字体加粗
        headWriteCellStyle.setWriteFont(headWriteFont); //在样式用应用设置的字体;
        // 样式
        headWriteCellStyle.setBorderBottom(BorderStyle.THIN);//设置底边框;
        headWriteCellStyle.setBottomBorderColor((short) 0);//设置底边框颜色;
        headWriteCellStyle.setBorderLeft(BorderStyle.THIN);  //设置左边框;
        headWriteCellStyle.setLeftBorderColor((short) 0);//设置左边框颜色;
        headWriteCellStyle.setBorderRight(BorderStyle.THIN);//设置右边框;
        headWriteCellStyle.setRightBorderColor((short) 0);//设置右边框颜色;
        headWriteCellStyle.setBorderTop(BorderStyle.THIN);//设置顶边框;
        headWriteCellStyle.setTopBorderColor((short) 0); //设置顶边框颜色;
        headWriteCellStyle.setWrapped(true);  //设置自动换行;
//        headWriteCellStyle.setHorizontalAlignment(HorizontalAlignment.CENTER);//设置水平对齐的样式为居中对齐;
//        headWriteCellStyle.setVerticalAlignment(VerticalAlignment.CENTER);  //设置垂直对齐的样式为居中对齐;
        //        headWriteCellStyle.setShrinkToFit(true);//设置文本收缩至合适

        return headWriteCellStyle;
    }

    /**
     * 在sheet创建完成后执行的操作，用于设置表格标题和样式
     * @param writeWorkbookHolder 工作簿持有者
     * @param writeSheetHolder sheet持有者
     */
    @Override
    public void afterSheetCreate(WriteWorkbookHolder writeWorkbookHolder, WriteSheetHolder writeSheetHolder) {
        // 可选：在创建sheet之后执行操作
        Workbook workbook = writeWorkbookHolder.getWorkbook();
        Sheet sheet = workbook.getSheetAt(0);
        //设置标题
        Row row1 = sheet.createRow(0);
        row1.setHeight((short) 800);
        Cell cell = row1.createCell(0);
        //设置单元格内容
        cell.setCellValue(title);
        CellStyle cellStyle = workbook.createCellStyle();
        cellStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        cellStyle.setAlignment(HorizontalAlignment.CENTER);
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeight((short) 400);
        cellStyle.setFont(font);
        cell.setCellStyle(cellStyle);
        // 第一行大标题占位设置
        sheet.addMergedRegionUnsafe(new CellRangeAddress(0, 0, 0, header.length - 1));
    }
}
