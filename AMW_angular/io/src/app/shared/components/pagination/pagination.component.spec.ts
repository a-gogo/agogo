import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { inject, TestBed } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { PaginationComponent } from './pagination.component';

@Component({
  template: ''
})
class DummyComponent {}

describe('PaginationComponent', () => {
  // provide our implementations or mocks to the dependency injector
  beforeEach(() =>
    TestBed.configureTestingModule({
      imports: [
        CommonModule,
        RouterTestingModule.withRoutes([
          { path: 'deployments', component: DummyComponent }
        ])
      ],
      providers: [PaginationComponent],
      declarations: [DummyComponent]
    })
  );

  it('should return no page numbers if there is just one page', inject(
    [PaginationComponent],
    (paginationComponent: PaginationComponent) => {
      // given
      paginationComponent.currentPage = 1;
      paginationComponent.lastPage = 1;

      // when
      const pages = paginationComponent.pages();

      // then
      expect(pages).toBeUndefined();
    }
  ));

  it('should return two page numbers if last page is 2', inject(
    [PaginationComponent],
    (paginationComponent: PaginationComponent) => {
      // given
      paginationComponent.currentPage = 1;
      paginationComponent.lastPage = 2;

      // when
      const pages = paginationComponent.pages();

      // then
      expect(pages.length).toEqual(2);
      expect(pages).toEqual([1, 2]);
    }
  ));

  it('should not return the right pages', inject(
    [PaginationComponent],
    (paginationComponent: PaginationComponent) => {
      // given
      paginationComponent.currentPage = 5;
      paginationComponent.paginatorItems = 5;
      paginationComponent.lastPage = paginationComponent.paginatorItems + 10;

      // when
      const pages = paginationComponent.pages();

      // then
      expect(pages.length).toEqual(paginationComponent.paginatorItems);
      expect(pages).toEqual([3, 4, 5, 6, 7]);
    }
  ));

  it('should emit the right offset on toPage', inject(
    [PaginationComponent],
    (paginationComponent: PaginationComponent) => {
      // given
      paginationComponent.currentPage = 1;
      paginationComponent.lastPage = paginationComponent.paginatorItems + 10;
      spyOn(paginationComponent.doSetOffset, 'emit');

      // when
      paginationComponent.toPage(2);

      // then
      expect(paginationComponent.doSetOffset.emit).toHaveBeenCalledWith(10);
    }
  ));

  it('should emit the right results offset when navigating to first page', inject(
    [PaginationComponent],
    (paginationComponent: PaginationComponent) => {
      // given
      paginationComponent.currentPage = 2;
      paginationComponent.lastPage = 10;
      spyOn(paginationComponent.doSetOffset, 'emit');

      // when
      paginationComponent.toPage(1);

      // then
      expect(paginationComponent.doSetOffset.emit).toHaveBeenCalledWith(0);
    }
  ));

  it('should emit the right results offset when navigating to second page', inject(
    [PaginationComponent],
    (paginationComponent: PaginationComponent) => {
      // given
      paginationComponent.currentPage = 1;
      paginationComponent.lastPage = 10;
      spyOn(paginationComponent.doSetOffset, 'emit');

      // when
      paginationComponent.toPage(2);

      // then
      expect(paginationComponent.doSetOffset.emit).toHaveBeenCalledWith(10);
    }
  ));

  it('should emit the right results offset depending on maxResults when navigating to second page', inject(
    [PaginationComponent],
    (paginationComponent: PaginationComponent) => {
      // given
      paginationComponent.currentPage = 1;
      paginationComponent.lastPage = 10;
      paginationComponent.maxResults = 50;
      spyOn(paginationComponent.doSetOffset, 'emit');

      // when
      paginationComponent.toPage(2);

      // then
      expect(paginationComponent.doSetOffset.emit).toHaveBeenCalledWith(50);
    }
  ));

  it('should emit the right results offset when navigating to last page', inject(
    [PaginationComponent],
    (paginationComponent: PaginationComponent) => {
      // given
      paginationComponent.currentPage = 1;
      paginationComponent.lastPage = 10;
      spyOn(paginationComponent.doSetOffset, 'emit');

      // when
      paginationComponent.toPage(10);

      // then
      expect(paginationComponent.doSetOffset.emit).toHaveBeenCalledWith(90);
    }
  ));
});
