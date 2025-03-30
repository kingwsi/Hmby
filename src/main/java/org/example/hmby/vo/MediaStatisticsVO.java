package org.example.hmby.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * description:  <br>
 * date: 2022/8/28 11:53 <br>
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class MediaStatisticsVO {
    private Double fileSize;
    private Double processedSize;
    private String date;
    private String type;
    private Double rate;
}
