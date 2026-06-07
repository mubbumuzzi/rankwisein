import { computed, inject, Injectable } from '@angular/core';
import { buildWhatsAppCounsellingUrl } from '../constants/site.constants';
import { PredictStateService } from './predict-state.service';

@Injectable({ providedIn: 'root' })
export class WhatsappCounsellingLinkService {
  private readonly predictState = inject(PredictStateService);

  /** WhatsApp link that updates when the student completes Check My Colleges. */
  readonly url = computed(() => buildWhatsAppCounsellingUrl(this.predictState.lastForm()));
}
