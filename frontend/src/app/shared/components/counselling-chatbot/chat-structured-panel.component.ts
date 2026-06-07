import { DecimalPipe } from '@angular/common';
import { Component, input } from '@angular/core';
import { ChatStructuredPayload } from '../../../core/models/ai-counsellor.models';

@Component({
  selector: 'app-chat-structured-panel',
  imports: [DecimalPipe],
  template: `
    @if (payload(); as p) {
      @if (p.type === 'PREDICTION' && p.prediction) {
        <div class="mt-2 space-y-2 rounded-xl border border-violet-100 bg-violet-50/50 p-3 text-xs">
          <p class="font-semibold text-violet-900">College prediction</p>
          @for (bucket of [
            { label: 'Dream', items: p.prediction.dream, cls: 'text-violet-800' },
            { label: 'Target', items: p.prediction.target, cls: 'text-sky-800' },
            { label: 'Safe', items: p.prediction.safe, cls: 'text-emerald-800' }
          ]; track bucket.label) {
            @if (bucket.items.length) {
              <div>
                <p class="font-bold" [class]="bucket.cls">{{ bucket.label }}</p>
                <ul class="mt-1 space-y-1">
                  @for (c of bucket.items; track c.collegeCode + c.branchCode) {
                    <li class="rounded-lg bg-white px-2 py-1.5 shadow-sm">
                      <span class="font-semibold text-rw-primary">{{ c.collegeName }}</span>
                      <span class="text-slate-500"> · {{ c.branchCode }}</span>
                      <div class="text-slate-600">
                        Closing {{ c.closingRank | number }} · Ratio {{ c.ratio }}
                        <span
                          class="ml-1 rounded px-1 py-0.5 text-[10px] font-bold uppercase"
                          [class]="c.confidence === 'HIGH' ? 'bg-emerald-100 text-emerald-800' : 'bg-amber-100 text-amber-800'"
                          >{{ c.confidence }}</span
                        >
                      </div>
                    </li>
                  }
                </ul>
              </div>
            }
          }
          <p class="text-[10px] text-slate-500">{{ p.prediction.confidenceNote }}</p>
        </div>
      }

      @if (p.type === 'COMPARISON' && p.comparison) {
        <div class="mt-2 grid gap-2 sm:grid-cols-2">
          @for (c of p.comparison.colleges; track c.code) {
            <div class="rounded-xl border border-slate-200 bg-white p-3 text-xs shadow-sm">
              <p class="font-bold text-rw-primary">{{ c.name }}</p>
              <p class="text-slate-500">{{ c.code }} · {{ c.location }}</p>
              <p class="mt-1"><span class="font-semibold">Cutoffs:</span> {{ c.cutoffSummary }}</p>
              <p class="mt-1 text-emerald-700"><span class="font-semibold">Pros:</span> {{ c.pros }}</p>
              <p class="mt-0.5 text-amber-700"><span class="font-semibold">Cons:</span> {{ c.cons }}</p>
            </div>
          }
        </div>
      }

      @if (p.type === 'DOCUMENTS' && p.documents) {
        <div class="mt-2 rounded-xl border border-sky-100 bg-sky-50/60 p-3 text-xs">
          <p class="font-bold text-sky-900">Required documents</p>
          <ul class="mt-1 list-inside list-disc text-slate-700">
            @for (d of p.documents.required; track d) {
              <li>{{ d }}</li>
            }
          </ul>
          @if (p.documents.optional.length) {
            <p class="mt-2 font-bold text-sky-900">Optional</p>
            <ul class="list-inside list-disc text-slate-600">
              @for (d of p.documents.optional; track d) {
                <li>{{ d }}</li>
              }
            </ul>
          }
          <p class="mt-2 text-[10px] text-slate-500">{{ p.documents.note }}</p>
        </div>
      }

      @if (p.type === 'BRANCH_ADVICE' && p.branchAdvice) {
        <div class="mt-2 rounded-xl border border-orange-100 bg-orange-50/60 p-3 text-xs">
          <p class="font-bold text-rw-accent">Suggested branches</p>
          <div class="mt-1 flex flex-wrap gap-1">
            @for (b of p.branchAdvice.recommendedBranches; track b) {
              <span class="rounded-full bg-white px-2 py-0.5 font-semibold text-rw-primary shadow-sm">{{
                b
              }}</span>
            }
          </div>
          <p class="mt-2 text-slate-600">{{ p.branchAdvice.reasoning }}</p>
        </div>
      }
    }
  `,
})
export class ChatStructuredPanelComponent {
  readonly payload = input<ChatStructuredPayload | null | undefined>(null);
}
