import { TestBed } from '@angular/core/testing';
import * as moment from 'moment';
import { DateTimePickerComponent } from './date-time-picker.component';
import { DateTimeModel } from "./date-time.model";

describe('DateTimeModel', () => {
  it('should convert dates correctly', () => {
    var fixture = TestBed.createComponent(DateTimePickerComponent);
    var component = fixture.componentInstance;
    var testDate = '02.01.2017 12:00';

    var m = moment(testDate, component.dateStringFormat);
    var dateTimeFromString = DateTimeModel.fromLocalString(testDate, component.dateStringFormat);
    var dateTimeFromEpoch = DateTimeModel.fromEpoch(m.valueOf());

    expect(dateTimeFromString.day).toEqual(m.date());
    expect(dateTimeFromString.month).toEqual(m.month() + 1);

    expect(dateTimeFromString).toEqual(dateTimeFromEpoch);

    expect(dateTimeFromString.toEpoch()).toEqual(m.valueOf());
  });
});
